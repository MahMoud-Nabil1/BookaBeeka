package com.system.booking.modules.notification.internal.listener;

import com.system.booking.modules.booking.internal.event.BookingConfirmedEvent;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Event-Driven Listener responsible for processing and dispatching notifications asynchronously.
 *
 * <h2>Architectural Deep Dive: Why Event-Driven Architecture (EDA)?</h2>
 * <p>
 * In a high-throughput multi-tenant SaaS booking engine, dispatching notifications (such as emails
 * or SMS) involves external network I/O, SMTP handshakes, TLS handshakes, and potential retry policies.
 * Tightly coupling notification delivery into the core business transactions (such as Booking Creation
 * or Payment Confirmation) introduces severe architectural risks:
 * </p>
 * <ol>
 *   <li><b>Blocking Request Latency:</b> Direct email delivery adds 500ms–3000ms to the user's HTTP request
 *       thread, drastically degrading user experience and saturating web server thread pools (e.g., Tomcat).</li>
 *   <li><b>Transaction Poisoning & False Failures:</b> If an external SMTP server times out or is temporarily
 *       down, an otherwise successful database transaction (e.g., room reserved, payment recorded) would be
 *       unjustly rolled back if executed synchronously within the same call stack.</li>
 *   <li><b>Module Decoupling:</b> The producer modules (Booking, Auth, Billing) only declare that an important
 *       business milestone occurred. They do not know, nor care, how the notification is dispatched, formatted,
 *       stored, or retried.</li>
 * </ol>
 *
 * <h2>Why {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)}?</h2>
 * <p>
 * Standard {@code @EventListener} executes immediately when an event is published, typically while the
 * database transaction is still active. If the transaction subsequently fails (e.g., optimistic locking conflict,
 * database constraint violation, or post-processing error), the customer would receive a "Booking Confirmed"
 * email for a booking that was rolled back and never existed in the database!
 * </p>
 * <p>
 * By specifying {@code phase = TransactionPhase.AFTER_COMMIT}, Spring guarantees that the event listener
 * is only invoked <b>after the surrounding database transaction has successfully committed to the database</b>.
 * We also configure {@code fallbackExecution = true} to gracefully handle events published outside an active
 * transaction (e.g., ephemeral OTP verification or stateless auth flows).
 * </p>
 *
 * <h2>Why {@code @Async} & Thread-Pool Multi-Tenancy Safety?</h2>
 * <p>
 * Even with {@code AFTER_COMMIT}, running synchronously would still block the HTTP thread after the commit.
 * {@link Async} delegates execution to Spring's background task executor.
 * </p>
 * <p><b>Critical Multi-Tenancy Isolation:</b> In Spring, {@link TenantContextHolder} relies on
 * {@link ThreadLocal} storage bound to the originating HTTP request thread. When execution transitions to
 * an {@code @Async} worker thread, the thread-local state is lost.
 * This listener bridges the multi-tenant context boundary by extracting {@code tenantId} from the
 * {@link NotificationEvent} and explicitly registering it in the worker thread via
 * {@link TenantContextHolder#setContext(TenantContext)}, ensuring that any downstream tenant-aware operations
 * remain strictly isolated. The context is guaranteed to be cleared in a {@code finally} block to prevent
 * thread-pool context leakage.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;

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
}
