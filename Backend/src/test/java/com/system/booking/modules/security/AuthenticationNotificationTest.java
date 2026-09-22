package com.system.booking.modules.security;

import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.dto.request.OtpRequest;
import com.system.booking.modules.security.dto.request.OtpVerificationRequest;
import com.system.booking.modules.security.dto.request.PasswordResetRequest;
import com.system.booking.modules.security.dto.request.ResetPasswordRequest;
import com.system.booking.modules.security.dto.response.OtpVerificationResponse;
import com.system.booking.modules.security.port.in.CustomerAuthPort;
import com.system.booking.modules.security.port.in.HotelAdminAuthPort;
import com.system.booking.modules.security.port.in.OwnerAuthPort;
import com.system.booking.modules.security.port.in.SuperAdminAuthPort;
import com.system.booking.modules.security.service.AuthenticationService;
import com.system.booking.modules.security.service.JwtService;
import com.system.booking.modules.security.service.SecurityTokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationNotificationTest {

    @Mock
    private SuperAdminAuthPort superAdminPort;

    @Mock
    private OwnerAuthPort ownerPort;

    @Mock
    private HotelAdminAuthPort hotelAdminPort;

    @Mock
    private CustomerAuthPort customerPort;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private SecurityTokenStore tokenStore = new SecurityTokenStore();

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        tokenStore.clear();
        authenticationService = new AuthenticationService(
                superAdminPort,
                ownerPort,
                hotelAdminPort,
                customerPort,
                passwordEncoder,
                jwtService,
                eventPublisher,
                tokenStore
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        tokenStore.clear();
    }

    // =========================================================================
    // OTP Request Tests
    // =========================================================================

    @Test
    @DisplayName("requestOtp should generate 6-digit OTP, cache it, and publish NotificationEvent")
    void testRequestOtpPublishesEventAndCachesCode() {
        UUID customerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "guest@example.com";

        AuthUserDTO user = new AuthUserDTO(customerId, email, "hash", "CUSTOMER", tenantId, true);
        when(customerPort.findCustomerByEmail(email)).thenReturn(Optional.of(user));

        authenticationService.requestOtp(new OtpRequest(email, tenantId));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.recipientEmail()).isEqualTo(email);
        assertThat(event.type()).isEqualTo(NotificationType.OTP_REQUESTED);
        assertThat(event.metadata()).containsKey("otpCode");

        String otpCode = (String) event.metadata().get("otpCode");
        assertThat(otpCode).hasSize(6).matches("^\\d{6}$");

        // Verify token store cached the OTP
        var cachedOtp = tokenStore.getOtp(email, tenantId);
        assertThat(cachedOtp).isPresent();
        assertThat(cachedOtp.get().code()).isEqualTo(otpCode);
        assertThat(cachedOtp.get().userId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("requestOtp should silently ignore non-existent email per OWASP guidelines")
    void testRequestOtpNonExistentEmailSilentIgnore() {
        String email = "unknown@example.com";
        when(customerPort.findCustomerByEmail(email)).thenReturn(Optional.empty());
        when(hotelAdminPort.findAdminByEmail(email)).thenReturn(Optional.empty());
        when(ownerPort.findOwnerByEmail(email)).thenReturn(Optional.empty());
        when(superAdminPort.findSuperAdminByEmail(email)).thenReturn(Optional.empty());

        authenticationService.requestOtp(new OtpRequest(email, null));

        verifyNoInteractions(eventPublisher);
        assertThat(tokenStore.getOtp(email, null)).isEmpty();
    }

    // =========================================================================
    // OTP Verification Tests
    // =========================================================================

    @Test
    @DisplayName("verifyOtp should successfully verify valid OTP and clean up cached token")
    void testVerifyOtpSuccess() {
        UUID customerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "guest@example.com";
        String otpCode = "123456";

        tokenStore.storeOtp(email, tenantId, customerId, otpCode);

        OtpVerificationResponse response = authenticationService.verifyOtp(
                new OtpVerificationRequest(email, otpCode, tenantId)
        );

        assertThat(response.verified()).isTrue();
        assertThat(response.message()).contains("successfully");

        // Token must be consumed (single-use guarantee)
        assertThat(tokenStore.getOtp(email, tenantId)).isEmpty();
        // Context must be cleared after execution
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("verifyOtp should throw BadCredentialsException on incorrect OTP")
    void testVerifyOtpIncorrectCode() {
        UUID customerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "guest@example.com";

        tokenStore.storeOtp(email, tenantId, customerId, "123456");

        assertThatThrownBy(() -> authenticationService.verifyOtp(
                new OtpVerificationRequest(email, "999999", tenantId)
        ))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid verification code");

        // Context must remain clear
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("verifyOtp should lock out after exceeding maximum allowed attempts")
    void testVerifyOtpMaxAttemptsLockout() {
        UUID customerId = UUID.randomUUID();
        String email = "guest@example.com";

        tokenStore.storeOtp(email, null, customerId, "123456");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authenticationService.verifyOtp(
                    new OtpVerificationRequest(email, "000000", null)
            )).isInstanceOf(BadCredentialsException.class);
        }

        // 6th attempt should result in lockout message and token deletion
        assertThatThrownBy(() -> authenticationService.verifyOtp(
                new OtpVerificationRequest(email, "123456", null)
        ))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Too many invalid attempts");

        assertThat(tokenStore.getOtp(email, null)).isEmpty();
    }

    // =========================================================================
    // Password Reset Request (Forgot Password) Tests
    // =========================================================================

    @Test
    @DisplayName("requestPasswordReset should generate token, cache it, and publish NotificationEvent")
    void testRequestPasswordResetPublishesEventAndCachesToken() {
        UUID customerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "customer@example.com";

        AuthUserDTO user = new AuthUserDTO(customerId, email, "oldHash", "CUSTOMER", tenantId, true);
        when(customerPort.findCustomerByEmail(email)).thenReturn(Optional.of(user));

        authenticationService.requestPasswordReset(new PasswordResetRequest(email, tenantId));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.recipientEmail()).isEqualTo(email);
        assertThat(event.type()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(event.metadata()).containsKey("resetToken");
        assertThat(event.metadata()).containsKey("resetUrl");

        String token = (String) event.metadata().get("resetToken");
        var cachedToken = tokenStore.getPasswordResetToken(token);
        assertThat(cachedToken).isPresent();
        assertThat(cachedToken.get().userId()).isEqualTo(customerId);
        assertThat(cachedToken.get().role()).isEqualTo("CUSTOMER");
        assertThat(cachedToken.get().email()).isEqualTo(email);
    }

    @Test
    @DisplayName("requestPasswordReset should silently ignore unknown email per OWASP")
    void testRequestPasswordResetUnknownEmailSilentIgnore() {
        String email = "nonexistent@example.com";
        when(customerPort.findCustomerByEmail(email)).thenReturn(Optional.empty());
        when(hotelAdminPort.findAdminByEmail(email)).thenReturn(Optional.empty());
        when(ownerPort.findOwnerByEmail(email)).thenReturn(Optional.empty());
        when(superAdminPort.findSuperAdminByEmail(email)).thenReturn(Optional.empty());

        authenticationService.requestPasswordReset(new PasswordResetRequest(email, null));

        verifyNoInteractions(eventPublisher);
    }

    // =========================================================================
    // Password Reset Execution (Reset Password) Tests
    // =========================================================================

    @Test
    @DisplayName("resetPassword should validate token, encode password, update DB via port, and consume token")
    void testResetPasswordCustomerSuccess() {
        UUID customerId = UUID.randomUUID();
        String email = "customer@example.com";
        String token = "valid-reset-token-123";
        String newPassword = "NewSecurePassword123!";

        tokenStore.storePasswordResetToken(token, customerId, "CUSTOMER", email, null);

        authenticationService.resetPassword(new ResetPasswordRequest(token, newPassword));

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(customerPort).updatePassword(eq(customerId), passwordCaptor.capture());

        String encodedHash = passwordCaptor.getValue();
        assertThat(passwordEncoder.matches(newPassword, encodedHash)).isTrue();

        // Token must be consumed
        assertThat(tokenStore.getPasswordResetToken(token)).isEmpty();
        // Context must be cleared
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("resetPassword should update HotelAdmin credentials and isolate tenant context")
    void testResetPasswordHotelAdminSuccess() {
        UUID adminId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "admin@hotel.com";
        String token = "admin-reset-token-456";
        String newPassword = "AdminNewPassword2026!";

        tokenStore.storePasswordResetToken(token, adminId, "ADMIN", email, tenantId);

        authenticationService.resetPassword(new ResetPasswordRequest(token, newPassword));

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(hotelAdminPort).updatePassword(eq(adminId), passwordCaptor.capture());

        String encodedHash = passwordCaptor.getValue();
        assertThat(passwordEncoder.matches(newPassword, encodedHash)).isTrue();
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("resetPassword should update Owner credentials and isolate tenant context")
    void testResetPasswordOwnerSuccess() {
        UUID ownerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "owner@hotel.com";
        String token = "owner-reset-token-789";
        String newPassword = "OwnerNewPassword2026!";

        tokenStore.storePasswordResetToken(token, ownerId, "OWNER", email, tenantId);

        authenticationService.resetPassword(new ResetPasswordRequest(token, newPassword));

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(ownerPort).updatePassword(eq(ownerId), passwordCaptor.capture());

        String encodedHash = passwordCaptor.getValue();
        assertThat(passwordEncoder.matches(newPassword, encodedHash)).isTrue();
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("resetPassword should update SuperAdmin credentials")
    void testResetPasswordSuperAdminSuccess() {
        UUID superAdminId = UUID.randomUUID();
        String email = "super@platform.com";
        String token = "super-reset-token-999";
        String newPassword = "SuperAdminPassword2026!";

        tokenStore.storePasswordResetToken(token, superAdminId, "SUPER_ADMIN", email, null);

        authenticationService.resetPassword(new ResetPasswordRequest(token, newPassword));

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(superAdminPort).updatePassword(eq(superAdminId), passwordCaptor.capture());

        String encodedHash = passwordCaptor.getValue();
        assertThat(passwordEncoder.matches(newPassword, encodedHash)).isTrue();
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("resetPassword should reject invalid or expired reset token")
    void testResetPasswordInvalidToken() {
        assertThatThrownBy(() -> authenticationService.resetPassword(
                new ResetPasswordRequest("invalid-token", "NewPassword123!")
        ))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid or expired password reset token");

        verifyNoInteractions(customerPort, hotelAdminPort, ownerPort, superAdminPort);
    }
}
