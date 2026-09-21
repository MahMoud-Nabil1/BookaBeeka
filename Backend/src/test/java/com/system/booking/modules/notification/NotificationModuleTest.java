package com.system.booking.modules.notification;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.event.BookingConfirmedEvent;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.notification.api.dto.NotificationResponseDto;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.entity.Notification;
import com.system.booking.modules.notification.internal.exception.EmailDispatchException;
import com.system.booking.modules.notification.internal.listener.NotificationEventListener;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import com.system.booking.modules.notification.internal.service.EmailSenderService;
import com.system.booking.modules.notification.internal.service.EmailTemplateService;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.security.context.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationModuleTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID tenantId;
    private UUID customerId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. NotificationEvent Record Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NotificationEvent should instantiate correctly with all valid attributes")
    void testNotificationEventValid() {
        NotificationEvent event = NotificationEvent.of(
                tenantId,
                customerId,
                bookingId,
                NotificationType.BOOKING_CONFIRMED,
                "Booking Confirmed",
                "customer@example.com",
                "Your booking is confirmed.",
                Map.of("key", "val")
        );

        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.bookingId()).isEqualTo(bookingId);
        assertThat(event.type()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(event.subject()).isEqualTo("Booking Confirmed");
        assertThat(event.recipientEmail()).isEqualTo("customer@example.com");
        assertThat(event.body()).isEqualTo("Your booking is confirmed.");
        assertThat(event.metadata()).containsEntry("key", "val");
    }

    @Test
    @DisplayName("NotificationEvent should reject null tenantId")
    void testNotificationEventNullTenant() {
        assertThatThrownBy(() -> NotificationEvent.of(
                null, customerId, bookingId, NotificationType.OTP_REQUESTED,
                "OTP", "test@test.com", "code"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("tenantId must not be null");
    }

    @Test
    @DisplayName("NotificationEvent should reject null customerId")
    void testNotificationEventNullCustomer() {
        assertThatThrownBy(() -> NotificationEvent.of(
                tenantId, null, bookingId, NotificationType.OTP_REQUESTED,
                "OTP", "test@test.com", "code"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("customerId must not be null");
    }

    @Test
    @DisplayName("NotificationEvent should reject blank email or subject")
    void testNotificationEventBlankFields() {
        assertThatThrownBy(() -> NotificationEvent.of(
                tenantId, customerId, bookingId, NotificationType.PASSWORD_RESET,
                "", "valid@email.com", "body"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("subject must not be blank");

        assertThatThrownBy(() -> NotificationEvent.of(
                tenantId, customerId, bookingId, NotificationType.PASSWORD_RESET,
                "Subject", "   ", "body"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("recipientEmail must not be blank");
    }

    // ─────────────────────────────────────────────────────────────
    // 2. NotificationService Tests (Creation, Template Rendering, Transitions)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("savePendingNotification should create entity in PENDING status with retryCount=0")
    void testSavePendingNotification() {
        Customer customerProxy = Customer.builder().id(customerId).build();
        Booking bookingProxy = Booking.builder().id(bookingId).build();

        when(entityManager.getReference(Customer.class, customerId)).thenReturn(customerProxy);
        when(entityManager.getReference(Booking.class, bookingId)).thenReturn(bookingProxy);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .customer(customerProxy)
                .booking(bookingProxy)
                .type(NotificationType.BOOKING_CONFIRMED)
                .subject("Booking Confirmed")
                .body("Your booking is confirmed")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationEvent event = NotificationEvent.of(
                tenantId, customerId, bookingId,
                NotificationType.BOOKING_CONFIRMED,
                "Booking Confirmed",
                "customer@example.com",
                "Your booking is confirmed"
        );

        Notification result = notificationService.savePendingNotification(event);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(result.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("markAsSent should transition status to SENT and populate sentAt")
    void testMarkAsSent() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markAsSent(notificationId);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isNotNull();
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("markAsFailed should transition status to FAILED and increment retryCount")
    void testMarkAsFailed() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markAsFailed(notificationId, "SMTP Timeout");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("SMTP Timeout");
        assertThat(result.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processNotification should render HTML template and transition to SENT on success")
    void testProcessNotificationSuccess() {
        UUID notificationId = UUID.randomUUID();
        Customer customerProxy = Customer.builder().id(customerId).build();

        when(emailTemplateService.renderEmail(any(), any())).thenReturn("<html>Rendered OTP</html>");
        when(entityManager.getReference(Customer.class, customerId)).thenReturn(customerProxy);

        Notification pendingNotification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .customer(customerProxy)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(pendingNotification);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification));

        NotificationEvent event = NotificationEvent.of(
                tenantId, customerId,
                NotificationType.OTP_REQUESTED,
                "Your OTP", "customer@example.com", "123456"
        );

        Notification finalState = notificationService.processNotification(event);

        verify(emailTemplateService).renderEmail(eq(NotificationType.OTP_REQUESTED), any());
        verify(emailSenderService).sendEmail("customer@example.com", "Your OTP", "<html>Rendered OTP</html>", true);
        assertThat(finalState.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(finalState.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("processNotification should transition to FAILED if email dispatch throws exception")
    void testProcessNotificationEmailFailure() {
        UUID notificationId = UUID.randomUUID();
        Customer customerProxy = Customer.builder().id(customerId).build();

        when(emailTemplateService.renderEmail(any(), any())).thenReturn("<html>Password Reset</html>");
        when(entityManager.getReference(Customer.class, customerId)).thenReturn(customerProxy);

        Notification pendingNotification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .customer(customerProxy)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(pendingNotification);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification));

        doThrow(new EmailDispatchException("Connection refused"))
                .when(emailSenderService).sendEmail(anyString(), anyString(), anyString(), anyBoolean());

        NotificationEvent event = NotificationEvent.of(
                tenantId, customerId,
                NotificationType.PASSWORD_RESET,
                "Password Reset", "customer@example.com", "Reset link"
        );

        Notification finalState = notificationService.processNotification(event);

        assertThat(finalState.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(finalState.getFailureReason()).isEqualTo("Connection refused");
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Retry Notification Logic
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("retryNotification should transition to SENT when retry email succeeds")
    void testRetryNotificationSuccess() {
        UUID notificationId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).email("retry@example.com").build();
        Notification failedNotification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .customer(customer)
                .subject("Test Subject")
                .body("<html>Body</html>")
                .status(NotificationStatus.FAILED)
                .retryCount(1)
                .failureReason("Timeout")
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(failedNotification));

        boolean result = notificationService.retryNotification(notificationId, 3);

        assertThat(result).isTrue();
        verify(emailSenderService).sendEmail("retry@example.com", "Test Subject", "<html>Body</html>", true);
        assertThat(failedNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(failedNotification.getSentAt()).isNotNull();
        assertThat(failedNotification.getFailureReason()).isNull();
        assertThat(failedNotification.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("retryNotification should record failure reason when retry email fails")
    void testRetryNotificationFailure() {
        UUID notificationId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).email("retry@example.com").build();
        Notification failedNotification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .customer(customer)
                .subject("Test Subject")
                .body("<html>Body</html>")
                .status(NotificationStatus.FAILED)
                .retryCount(2)
                .failureReason("Timeout")
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(failedNotification));
        doThrow(new EmailDispatchException("Host unreachable"))
                .when(emailSenderService).sendEmail(anyString(), anyString(), anyString(), anyBoolean());

        boolean result = notificationService.retryNotification(notificationId, 3);

        assertThat(result).isFalse();
        assertThat(failedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failedNotification.getFailureReason()).isEqualTo("Host unreachable");
        assertThat(failedNotification.getRetryCount()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. NotificationEventListener & Tenant Context Propagation
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NotificationEventListener should establish TenantContext and clear it in finally")
    void testListenerTenantContextPropagation() {
        NotificationService mockService = mock(NotificationService.class);
        NotificationEventListener testListener = new NotificationEventListener(mockService, customerRepository);

        NotificationEvent event = NotificationEvent.of(
                tenantId, customerId,
                NotificationType.BOOKING_CONFIRMED,
                "Subject", "cust@example.com", "body"
        );

        doAnswer(invocation -> {
            assertThat(TenantContextHolder.getContext()).isNotNull();
            assertThat(TenantContextHolder.getContext().tenantId()).isEqualTo(tenantId);
            return null;
        }).when(mockService).processNotification(event);

        testListener.onNotificationEvent(event);

        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("NotificationEventListener should handle BookingConfirmedEvent and translate to notification")
    void testListenerBookingConfirmedDomainEvent() {
        NotificationService mockService = mock(NotificationService.class);
        Customer customer = Customer.builder()
                .id(customerId)
                .email("guest@hotel.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        NotificationEventListener testListener = new NotificationEventListener(mockService, customerRepository);

        BookingConfirmedEvent domainEvent = new BookingConfirmedEvent(
                bookingId,
                tenantId,
                customerId,
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2)
        );

        testListener.onBookingConfirmed(domainEvent);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(mockService).processNotification(captor.capture());

        NotificationEvent published = captor.getValue();
        assertThat(published.tenantId()).isEqualTo(tenantId);
        assertThat(published.customerId()).isEqualTo(customerId);
        assertThat(published.bookingId()).isEqualTo(bookingId);
        assertThat(published.recipientEmail()).isEqualTo("guest@hotel.com");
        assertThat(published.type()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(published.subject()).contains("Booking Confirmed");
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    // ─────────────────────────────────────────────────────────────
    // 5. NotificationResponseDto Mapping
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NotificationResponseDto.fromEntity should correctly map and flatten entities")
    void testResponseDtoMapping() {
        Customer customer = Customer.builder().id(customerId).build();
        Booking booking = Booking.builder().id(bookingId).build();
        UUID notificationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Notification entity = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .customer(customer)
                .booking(booking)
                .type(NotificationType.BOOKING_CONFIRMED)
                .subject("Test Subject")
                .body("Test Body")
                .status(NotificationStatus.SENT)
                .sentAt(now)
                .createdAt(now)
                .retryCount(1)
                .failureReason(null)
                .build();

        NotificationResponseDto dto = NotificationResponseDto.fromEntity(entity);

        assertThat(dto.id()).isEqualTo(notificationId);
        assertThat(dto.tenantId()).isEqualTo(tenantId);
        assertThat(dto.customerId()).isEqualTo(customerId);
        assertThat(dto.bookingId()).isEqualTo(bookingId);
        assertThat(dto.type()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(dto.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(dto.sentAt()).isEqualTo(now);
        assertThat(dto.retryCount()).isEqualTo(1);
    }
}
