package com.system.booking.modules.notification.internal.listener;

import com.system.booking.modules.booking.internal.event.BookingCancelledEvent;
import com.system.booking.modules.booking.internal.event.BookingConfirmedEvent;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.tenant.api.TenantModuleApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Event-Driven Listener responsible for processing and dispatching notifications asynchronously.
 */
@Component
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final OwnerRepository ownerRepository;
    private final TenantModuleApi tenantModuleApi;
    private final InventoryModuleApi inventoryApi;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationEventListener(NotificationService notificationService,
                                   CustomerRepository customerRepository,
                                   OwnerRepository ownerRepository,
                                   TenantModuleApi tenantModuleApi,
                                   InventoryModuleApi inventoryApi) {
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
        this.ownerRepository = ownerRepository;
        this.tenantModuleApi = tenantModuleApi;
        this.inventoryApi = inventoryApi;
    }

    /**
     * Backward-compatible constructor for existing tests that mock only core notification dependencies.
     */
    public NotificationEventListener(NotificationService notificationService,
                                   CustomerRepository customerRepository) {
        this(notificationService, customerRepository, null, null, null);
    }

    /**
     * Listens to explicitly published {@link NotificationEvent} instances.
     * Executes in a separate thread pool strictly after the emitting transaction commits.
     *
     * @param event The notification event payload.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationEvent(NotificationEvent event) {
        log.info("[ASYNC_LISTENER] Received NotificationEvent: type=[{}], recipient=[{}], tenant=[{}]",
                event.type(), event.recipientEmail(), event.tenantId());

        // Explicitly establish tenant context on the worker thread
        TenantContextHolder.setContext(new TenantContext(event.tenantId()));
        try {
            notificationService.processNotification(event);
        } catch (Exception ex) {
            log.error("[ASYNC_LISTENER] Uncaught error during notification processing: {}", ex.getMessage(), ex);
        } finally {
            // Guarantee ThreadLocal cleanup to prevent thread pool leakage
            TenantContextHolder.clear();
        }
    }

    /**
     * Domain event listener listening to {@link BookingConfirmedEvent} published by the Booking module.
     * Demonstrates seamless cross-module event choreography where domain events are translated
     * into notification delivery without tight coupling.
     *
     * @param event The booking confirmation domain event.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[ASYNC_LISTENER] Received BookingConfirmedEvent for bookingId=[{}], customerId=[{}], tenant=[{}]",
                event.bookingId(), event.customerId(), event.tenantId());

        TenantContextHolder.setContext(new TenantContext(event.tenantId()));
        try {
            Customer customer = customerRepository.findById(event.customerId()).orElse(null);
            if (customer == null) {
                log.warn("[ASYNC_LISTENER] Cannot send booking confirmation: Customer [{}] not found", event.customerId());
                return;
            }

            String subject = "Booking Confirmed - Hotel Reservation #" + event.bookingId().toString().substring(0, 8);
            String body = String.format(
                    "Dear %s %s,%n%n" +
                    "Your booking has been successfully confirmed!%n%n" +
                    "Booking ID: %s%n" +
                    "Check-In / Slot Start: %s%n" +
                    "Check-Out / Slot End: %s%n%n" +
                    "Thank you for choosing BookaBeeka!",
                    customer.getFirstName(), customer.getLastName(),
                    event.bookingId(),
                    event.slotStart(),
                    event.slotEnd()
            );

            NotificationEvent notificationEvent = NotificationEvent.of(
                    event.tenantId(),
                    event.customerId(),
                    event.bookingId(),
                    NotificationType.BOOKING_CONFIRMED,
                    subject,
                    customer.getEmail(),
                    body,
                    Map.of(
                            "bookingId", event.bookingId().toString(),
                            "roomId", event.roomId().toString()
                    )
            );

            notificationService.processNotification(notificationEvent);

        } catch (Exception ex) {
            log.error("[ASYNC_LISTENER] Error processing BookingConfirmedEvent: {}", ex.getMessage(), ex);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Domain event listener listening to {@link BookingCancelledEvent} published by the Booking module.
     * Executes asynchronously in a background thread pool strictly AFTER the cancellation transaction has committed.
     * Dispatches cancellation notices to the customer, and to the property owner if cancellation was
     * initiated by the customer or an automated system.
     *
     * @param event The booking cancelled domain event.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("[ASYNC_LISTENER] Received BookingCancelledEvent for bookingId=[{}], tenant=[{}]",
                event.bookingId(), event.tenantId());

        if (event.tenantId() != null) {
            TenantContextHolder.setContext(new TenantContext(event.tenantId()));
        }

        try {
            Customer customer = (event.customerId() != null)
                    ? customerRepository.findById(event.customerId()).orElse(null)
                    : null;

            if (customer == null) {
                log.warn("[ASYNC_LISTENER] Cannot send cancellation notice: Customer [{}] not found for booking [{}]",
                        event.customerId(), event.bookingId());
                return;
            }

            // Resolve Property & Room Details (with graceful fallbacks)
            String hotelName = "BookaBeeka";
            try {
                if (tenantModuleApi != null && event.tenantId() != null) {
                    var tenant = tenantModuleApi.getTenantById(event.tenantId());
                    if (tenant != null && tenant.name() != null && !tenant.name().isBlank()) {
                        hotelName = tenant.name();
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not resolve hotel name for tenant [{}]: {}", event.tenantId(), ex.getMessage());
            }

            String roomName = null;
            try {
                if (inventoryApi != null && event.tenantId() != null && event.roomId() != null) {
                    var resource = inventoryApi.getResourceByTenantAndId(event.tenantId(), event.roomId());
                    if (resource != null) {
                        roomName = resource.name();
                        if (resource.roomNumber() != null && !resource.roomNumber().isBlank()) {
                            roomName += " (Room " + resource.roomNumber() + ")";
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not resolve room name for room [{}]: {}", event.roomId(), ex.getMessage());
            }

            // Determine neutral actor description
            String cancelledBy;
            boolean isCancelledByCustomer = event.actorId() != null && event.actorId().equals(event.customerId());
            if (isCancelledByCustomer) {
                cancelledBy = "Guest request";
            } else if (event.actorId() != null) {
                cancelledBy = "Hotel Management";
            } else {
                cancelledBy = "Automated System";
            }

            // Determine refund description based on actual data
            String refundText;
            if (event.refundAmount() != null && event.refundAmount().compareTo(BigDecimal.ZERO) > 0) {
                int pct = event.refundPercentage() != null ? event.refundPercentage() : 0;
                String cur = event.currency() != null ? event.currency() : "$";
                refundText = String.format("%s %.2f (%d%% refund per policy)", cur, event.refundAmount(), pct);
            } else if (event.totalAmount() != null && event.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
                refundText = "Non-refundable (0% refund based on cancellation policy)";
            } else {
                refundText = "No payment was collected (no refund needed)";
            }

            String bookingRef = "#" + event.bookingId().toString().substring(0, 8).toUpperCase();
            String checkInStr = event.checkIn() != null ? event.checkIn().toString()
                    : (event.slotStart() != null ? event.slotStart().toLocalDate().toString() : "N/A");
            String checkOutStr = event.checkOut() != null ? event.checkOut().toString()
                    : (event.slotEnd() != null ? event.slotEnd().toLocalDate().toString() : "N/A");
            String cancelledAtStr = event.cancelledAt() != null
                    ? event.cancelledAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))
                    : OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"));

            // 1. Dispatch cancellation email to the Guest
            String guestSubject = "Your booking " + bookingRef + " has been cancelled";
            String guestBody = String.format(
                    "Dear %s %s,%n%nYour booking %s at %s has been cancelled.%n%n" +
                    "Check-In: %s%nCheck-Out: %s%nCancelled By: %s%nCancellation Time: %s%nReason: %s%nRefund: %s%n%n" +
                    "Thank you for choosing BookaBeeka.",
                    customer.getFirstName(), customer.getLastName(),
                    bookingRef, hotelName,
                    checkInStr, checkOutStr, cancelledBy, cancelledAtStr,
                    event.reason() != null ? event.reason() : "N/A",
                    refundText
            );

            Map<String, Object> guestMetadata = new HashMap<>();
            guestMetadata.put("bookingId", event.bookingId().toString());
            guestMetadata.put("bookingReference", bookingRef);
            guestMetadata.put("recipientName", customer.getFirstName() + " " + customer.getLastName());
            guestMetadata.put("hotelName", hotelName);
            if (roomName != null) guestMetadata.put("roomName", roomName);
            guestMetadata.put("checkIn", checkInStr);
            guestMetadata.put("checkOut", checkOutStr);
            guestMetadata.put("cancelledBy", cancelledBy);
            guestMetadata.put("cancelledAt", cancelledAtStr);
            if (event.reason() != null) guestMetadata.put("reason", event.reason());
            guestMetadata.put("refundText", refundText);

            NotificationEvent guestNotification = NotificationEvent.of(
                    event.tenantId(),
                    event.customerId(),
                    event.bookingId(),
                    NotificationType.BOOKING_CANCELLED,
                    guestSubject,
                    customer.getEmail(),
                    guestBody,
                    guestMetadata
            );

            notificationService.processNotification(guestNotification);

            // 2. Dispatch cancellation notice to the Property Owner (if cancelled by customer or automated system)
            if (ownerRepository != null && (isCancelledByCustomer || event.actorId() == null)) {
                try {
                    var ownerOpt = ownerRepository.findByTenantId(event.tenantId());
                    if (ownerOpt.isPresent()) {
                        var owner = ownerOpt.get();
                        if (owner.getEmail() != null && !owner.getEmail().isBlank()) {
                            String ownerSubject = "[Cancellation Notice] Booking " + bookingRef + " cancelled";
                            String ownerBody = String.format(
                                    "Hello %s,%n%nBooking %s for guest %s %s at %s has been cancelled.%n%n" +
                                    "Check-In: %s%nCheck-Out: %s%nCancelled By: %s%nReason: %s%nRefund: %s%n",
                                    owner.getFirstName(),
                                    bookingRef, customer.getFirstName(), customer.getLastName(), hotelName,
                                    checkInStr, checkOutStr, cancelledBy,
                                    event.reason() != null ? event.reason() : "N/A",
                                    refundText
                            );

                            Map<String, Object> ownerMetadata = new HashMap<>(guestMetadata);
                            ownerMetadata.put("recipientName", owner.getFirstName() + " " + owner.getLastName());

                            NotificationEvent ownerNotification = NotificationEvent.of(
                                    event.tenantId(),
                                    null, // owner is not a Customer entity
                                    event.bookingId(),
                                    NotificationType.BOOKING_CANCELLED,
                                    ownerSubject,
                                    owner.getEmail(),
                                    ownerBody,
                                    ownerMetadata
                            );

                            notificationService.processNotification(ownerNotification);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("[ASYNC_LISTENER] Failed to notify owner for cancelled booking [{}]: {}",
                            event.bookingId(), ex.getMessage());
                }
            }

        } catch (Exception ex) {
            log.error("[ASYNC_LISTENER] Error processing BookingCancelledEvent for bookingId [{}]: {}",
                    event.bookingId(), ex.getMessage(), ex);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
