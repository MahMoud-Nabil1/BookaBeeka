package com.system.booking.modules.booking;

import com.system.booking.modules.availability.api.AvailabilityModuleApi;
import com.system.booking.modules.booking.api.CancellationResultDto;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.event.BookingCancelledEvent;
import com.system.booking.modules.booking.internal.exception.IllegalBookingStateTransitionException;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.booking.internal.repository.BookingStateTransitionRepository;
import com.system.booking.modules.booking.internal.service.BookingLifecycleService;
import com.system.booking.modules.booking.internal.service.BookingStateMachine;
import com.system.booking.modules.booking.internal.service.CancellationPolicyService;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.listener.NotificationEventListener;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.owner.internal.entity.Owner;
import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.tenant.api.TenantModuleApi;
import com.system.booking.modules.tenant.internal.dto.TenantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCancellationNotificationTest {

    // Lifecycle Mocks
    @Mock private BookingRepository bookingRepo;
    @Mock private BookingStateTransitionRepository transitionRepo;
    @Mock private CancellationPolicyService cancellationPolicyService;
    @Mock private AvailabilityModuleApi availabilityApi;
    @Mock private ApplicationEventPublisher eventPublisher;

    private final BookingStateMachine stateMachine = new BookingStateMachine();
    private BookingLifecycleService lifecycleService;

    // Listener Mocks
    @Mock private NotificationService notificationService;
    @Mock private CustomerRepository customerRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private TenantModuleApi tenantModuleApi;
    @Mock private InventoryModuleApi inventoryApi;

    private NotificationEventListener listener;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lifecycleService = new BookingLifecycleService(
                bookingRepo,
                transitionRepo,
                stateMachine,
                cancellationPolicyService,
                availabilityApi,
                eventPublisher
        );

        listener = new NotificationEventListener(
                notificationService,
                customerRepository,
                ownerRepository,
                tenantModuleApi,
                inventoryApi
        );
    }

    @Test
    @DisplayName("cancelBooking: publishes enriched BookingCancelledEvent with correct metadata")
    void cancelBooking_publishesEnrichedEvent() {
        // Arrange
        Booking booking = Booking.builder()
                .customerId(customerId)
                .roomId(roomId)
                .checkIn(LocalDate.of(2026, 11, 1))
                .checkOut(LocalDate.of(2026, 11, 5))
                .startTime(OffsetDateTime.now().plusDays(20))
                .endTime(OffsetDateTime.now().plusDays(24))
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("300.00"))
                .currency("USD")
                .build();
        booking.setId(bookingId);
        booking.setTenantId(tenantId);

        when(bookingRepo.findByTenantIdAndId(tenantId, bookingId)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.calculateRefundPercentage(eq(booking), any())).thenReturn(100);
        when(cancellationPolicyService.calculateRefundAmount(eq(booking), any())).thenReturn(new BigDecimal("300.00"));

        // Act
        CancellationResultDto result = lifecycleService.cancelBooking(
                tenantId, bookingId, "Flight cancelled", customerId);

        // Assert
        assertThat(result.newStatus()).isEqualTo("CANCELLED");
        assertThat(result.refundAmount()).isEqualByComparingTo("300.00");
        assertThat(result.refundPercentage()).isEqualTo(100);

        ArgumentCaptor<BookingCancelledEvent> eventCaptor = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        BookingCancelledEvent captured = eventCaptor.getValue();
        assertThat(captured.bookingId()).isEqualTo(bookingId);
        assertThat(captured.tenantId()).isEqualTo(tenantId);
        assertThat(captured.customerId()).isEqualTo(customerId);
        assertThat(captured.roomId()).isEqualTo(roomId);
        assertThat(captured.actorId()).isEqualTo(customerId);
        assertThat(captured.refundAmount()).isEqualByComparingTo("300.00");
        assertThat(captured.refundPercentage()).isEqualTo(100);
        assertThat(captured.reason()).isEqualTo("Flight cancelled");
        assertThat(captured.checkIn()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(captured.checkOut()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("cancelBooking: cancelling already-cancelled booking throws exception and never publishes duplicate event")
    void cancelBooking_alreadyCancelled_throwsException_neverPublishesEvent() {
        // Arrange
        Booking booking = Booking.builder()
                .customerId(customerId)
                .roomId(roomId)
                .status(BookingStatus.CANCELLED)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        booking.setId(bookingId);
        booking.setTenantId(tenantId);

        when(bookingRepo.findByTenantIdAndId(tenantId, bookingId)).thenReturn(Optional.of(booking));

        // Act & Assert
        assertThatThrownBy(() -> lifecycleService.cancelBooking(tenantId, bookingId, "Try cancel again", customerId))
                .isInstanceOf(IllegalBookingStateTransitionException.class);

        verify(eventPublisher, never()).publishEvent(any());
        verify(bookingRepo, never()).save(any());
    }

    @Test
    @DisplayName("onBookingCancelled: guest cancellation triggers emails to both guest and property owner")
    void onBookingCancelled_byGuest_notifiesBothGuestAndOwner() {
        // Arrange
        Customer customer = Customer.builder()
                .id(customerId)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@guest.com")
                .build();

        Owner owner = Owner.builder()
                .id(ownerId)
                .tenantId(tenantId)
                .firstName("Bob")
                .lastName("Hotelier")
                .email("owner@grandhotel.com")
                .build();

        TenantDto tenantDto = new TenantDto(
                tenantId, "Grand Palace Hotel", "grandpalace", "ACTIVE", null, "UTC", "USD", null, null);

        ResourceResponse resourceResponse = new ResourceResponse(
                roomId, tenantId, UUID.randomUUID(), "Deluxe Room", "Seaside Suite", "101",
                1, null, "ROOM", 2, null, true, true, new BigDecimal("150.00"), "USD", null);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(tenantModuleApi.getTenantById(tenantId)).thenReturn(tenantDto);
        when(inventoryApi.getResourceByTenantAndId(tenantId, roomId)).thenReturn(resourceResponse);
        when(ownerRepository.findByTenantId(tenantId)).thenReturn(Optional.of(owner));

        BookingCancelledEvent event = new BookingCancelledEvent(
                bookingId,
                tenantId,
                customerId,
                roomId,
                customerId, // actor is guest
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15),
                OffsetDateTime.now().plusDays(30),
                OffsetDateTime.now().plusDays(35),
                new BigDecimal("500.00"),
                "USD",
                new BigDecimal("500.00"),
                100,
                "Change of plans",
                OffsetDateTime.now()
        );

        // Act
        listener.onBookingCancelled(event);

        // Assert: 2 notifications dispatched (guest + owner)
        ArgumentCaptor<NotificationEvent> notifCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService, times(2)).processNotification(notifCaptor.capture());

        List<NotificationEvent> dispatched = notifCaptor.getAllValues();

        // 1. Guest Notification
        NotificationEvent guestNotif = dispatched.stream()
                .filter(n -> "alice@guest.com".equals(n.recipientEmail()))
                .findFirst()
                .orElseThrow();
        assertThat(guestNotif.type()).isEqualTo(NotificationType.BOOKING_CANCELLED);
        assertThat(guestNotif.subject()).contains("has been cancelled");
        assertThat(guestNotif.metadata()).containsEntry("hotelName", "Grand Palace Hotel");
        assertThat(guestNotif.metadata().get("roomName")).toString().contains("Seaside Suite");
        assertThat(guestNotif.metadata()).containsEntry("checkIn", "2026-12-10");
        assertThat(guestNotif.metadata()).containsEntry("checkOut", "2026-12-15");
        assertThat(guestNotif.metadata()).containsEntry("cancelledBy", "Guest request");
        assertThat(guestNotif.metadata().get("refundText")).toString().contains("100% refund per policy");

        // 2. Owner Notification
        NotificationEvent ownerNotif = dispatched.stream()
                .filter(n -> "owner@grandhotel.com".equals(n.recipientEmail()))
                .findFirst()
                .orElseThrow();
        assertThat(ownerNotif.type()).isEqualTo(NotificationType.BOOKING_CANCELLED);
        assertThat(ownerNotif.subject()).contains("[Cancellation Notice]");
        assertThat(ownerNotif.metadata()).containsEntry("recipientName", "Bob Hotelier");
    }

    @Test
    @DisplayName("onBookingCancelled: staff cancellation notifies guest, but does NOT send duplicate notice to owner")
    void onBookingCancelled_byStaff_notifiesGuestOnly() {
        // Arrange
        Customer customer = Customer.builder()
                .id(customerId)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@guest.com")
                .build();

        UUID staffActorId = UUID.randomUUID(); // Staff actor (not customer)

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(tenantModuleApi.getTenantById(tenantId)).thenReturn(new TenantDto(
                tenantId, "Boutique Inn", "boutique", "ACTIVE", null, "UTC", "USD", null, null));

        BookingCancelledEvent event = new BookingCancelledEvent(
                bookingId,
                tenantId,
                customerId,
                roomId,
                staffActorId, // actor is staff/owner
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15),
                OffsetDateTime.now().plusDays(30),
                OffsetDateTime.now().plusDays(35),
                new BigDecimal("200.00"),
                "USD",
                BigDecimal.ZERO,
                0,
                "Hotel maintenance issue",
                OffsetDateTime.now()
        );

        // Act
        listener.onBookingCancelled(event);

        // Assert: exactly 1 notification dispatched to guest, owner is NOT messaged
        ArgumentCaptor<NotificationEvent> notifCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService, times(1)).processNotification(notifCaptor.capture());

        NotificationEvent guestNotif = notifCaptor.getValue();
        assertThat(guestNotif.recipientEmail()).isEqualTo("alice@guest.com");
        assertThat(guestNotif.metadata()).containsEntry("cancelledBy", "Hotel Management");
        assertThat(guestNotif.metadata().get("refundText")).toString().contains("0% refund based on cancellation policy");

        verifyNoInteractions(ownerRepository);
    }

    @Test
    @DisplayName("onBookingCancelled: exception during notification delivery is isolated and does not propagate")
    void onBookingCancelled_deliveryFailure_isIsolatedAndLogged() {
        // Arrange
        Customer customer = Customer.builder()
                .id(customerId)
                .firstName("Carol")
                .lastName("Danvers")
                .email("carol@hero.com")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doThrow(new RuntimeException("Mail server down"))
                .when(notificationService).processNotification(any());

        BookingCancelledEvent event = new BookingCancelledEvent(
                bookingId, tenantId, customerId, roomId, customerId,
                LocalDate.now(), LocalDate.now().plusDays(2),
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(2),
                BigDecimal.TEN, "USD", BigDecimal.ZERO, 0, "Emergency", OffsetDateTime.now()
        );

        // Act & Assert: does not throw out of listener
        listener.onBookingCancelled(event);

        verify(notificationService).processNotification(any());
    }
}
