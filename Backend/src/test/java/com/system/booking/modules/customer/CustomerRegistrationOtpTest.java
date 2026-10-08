package com.system.booking.modules.customer;

import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.customer.internal.service.CustomerService;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.security.service.SecurityTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying that customer registration triggers OTP generation and email dispatch.
 *
 * <p>All external dependencies (repository, publisher, token store) are mocked so this
 * test is fast and does not require a running database or SMTP server.</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerRegistrationOtpTest {

    @Mock CustomerRepository customerRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock SecurityTokenStore tokenStore;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private CustomerService customerService;

    private static final UUID PLATFORM_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                customerRepository, passwordEncoder, eventPublisher, tokenStore);
    }

    @Test
    @DisplayName("registerCustomer: OTP is stored in SecurityTokenStore after save")
    void registration_storesOtpInTokenStore() {
        // Arrange
        CustomerRegisterRequest req = new CustomerRegisterRequest(
                "Alice", "Smith", "alice@example.com", "password123", "+1234567890");

        Customer saved = new Customer();
        saved.setId(UUID.randomUUID());
        saved.setEmail("alice@example.com");
        saved.setFirstName("Alice");

        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(saved);

        // Act
        customerService.registerCustomer(req);

        // Assert: OTP stored with platform tenant ID and saved customer's ID
        verify(tokenStore).storeOtp(
                eq("alice@example.com"),
                eq(PLATFORM_TENANT_ID),
                eq(saved.getId()),
                any(String.class)          // OTP value — not asserted (random)
        );
    }

    @Test
    @DisplayName("registerCustomer: NotificationEvent of type OTP_REQUESTED is published after save")
    void registration_publishesOtpNotificationEvent() {
        // Arrange
        CustomerRegisterRequest req = new CustomerRegisterRequest(
                "Bob", "Jones", "bob@example.com", "secret99", null);

        Customer saved = new Customer();
        saved.setId(UUID.randomUUID());
        saved.setEmail("bob@example.com");
        saved.setFirstName("Bob");

        when(customerRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(saved);

        ArgumentCaptor<NotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationEvent.class);

        // Act
        customerService.registerCustomer(req);

        // Assert: event published with correct type and recipient
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NotificationEvent event = eventCaptor.getValue();

        assertThat(event.type()).isEqualTo(NotificationType.OTP_REQUESTED);
        assertThat(event.recipientEmail()).isEqualTo("bob@example.com");
        assertThat(event.tenantId()).isEqualTo(PLATFORM_TENANT_ID);
        assertThat(event.customerId()).isEqualTo(saved.getId());
        assertThat(event.metadata()).containsKey("otpCode");
        assertThat(event.metadata().get("otpCode").toString()).matches("\\d{6}");
    }

    @Test
    @DisplayName("registerCustomer: duplicate email throws IllegalArgumentException — no OTP sent")
    void registration_duplicateEmail_throwsAndNeverSendsOtp() {
        // Arrange
        CustomerRegisterRequest req = new CustomerRegisterRequest(
                "Carol", "White", "carol@example.com", "pass1234", null);

        when(customerRepository.existsByEmail("carol@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.registerCustomer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

        // No OTP stored or event published
        verifyNoInteractions(tokenStore, eventPublisher);
    }

    @Test
    @DisplayName("registerCustomer: OTP metadata contains 6-digit numeric code")
    void registration_otpCodeIs6Digits() {
        // Arrange
        CustomerRegisterRequest req = new CustomerRegisterRequest(
                "Dan", "Fox", "dan@example.com", "hunter2", null);

        Customer saved = new Customer();
        saved.setId(UUID.randomUUID());
        saved.setEmail("dan@example.com");
        saved.setFirstName("Dan");

        when(customerRepository.existsByEmail("dan@example.com")).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(saved);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);

        // Act
        customerService.registerCustomer(req);

        // Assert: OTP is exactly 6 digits
        verify(eventPublisher).publishEvent(captor.capture());
        String otp = captor.getValue().metadata().get("otpCode").toString();
        assertThat(otp).matches("^\\d{6}$");
    }
}
