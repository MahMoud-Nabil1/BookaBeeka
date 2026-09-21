package com.system.booking.modules.security.service;

import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.dto.request.LoginRequest;
import com.system.booking.modules.security.dto.response.LoginResponse;
import com.system.booking.modules.security.port.in.CustomerAuthPort;
import com.system.booking.modules.security.port.in.HotelAdminAuthPort;
import com.system.booking.modules.security.port.in.OwnerAuthPort;
import com.system.booking.modules.security.port.in.SuperAdminAuthPort;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.security.dto.request.OtpRequest;
import com.system.booking.modules.security.dto.request.PasswordResetRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private static final UUID PLATFORM_FALLBACK_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final SuperAdminAuthPort superAdminPort;
    private final OwnerAuthPort ownerPort;
    private final HotelAdminAuthPort hotelAdminPort;
    private final CustomerAuthPort customerPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    public LoginResponse loginSuperAdmin(LoginRequest request) {
        var user = superAdminPort.findSuperAdminByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    public LoginResponse loginOwner(LoginRequest request) {
        var user = ownerPort.findOwnerByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    public LoginResponse loginAdmin(LoginRequest request) {
        var user = hotelAdminPort.findAdminByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    public LoginResponse loginCustomer(LoginRequest request) {
        var user = customerPort.findCustomerByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    /**
     * Generates a 6-digit OTP code and dispatches a NotificationEvent.
     *
     * @param request The OTP request payload containing recipient email and optional tenantId.
     */
    public void requestOtp(OtpRequest request) {
        findUserByEmail(request.email()).ifPresentOrElse(user -> {
            String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            UUID tenantId = (request.tenantId() != null)
                    ? request.tenantId()
                    : (user.tenantId() != null ? user.tenantId() : PLATFORM_FALLBACK_TENANT_ID);

            log.info("Publishing OTP NotificationEvent for user [{}] under tenant [{}]", user.email(), tenantId);

            NotificationEvent event = NotificationEvent.of(
                    tenantId,
                    user.id(),
                    null,
                    NotificationType.OTP_REQUESTED,
                    "Your Verification Code - BookaBeeka",
                    user.email(),
                    "Your one-time verification code is: " + otpCode,
                    Map.of(
                            "otpCode", otpCode,
                            "customerName", user.email(),
                            "expiryMinutes", 10
                    )
            );
            eventPublisher.publishEvent(event);

        }, () -> log.warn("OTP requested for non-existent email [{}]. Silently ignored per OWASP.", request.email()));
    }

    /**
     * Generates a secure password reset token and dispatches a NotificationEvent.
     *
     * @param request The password reset request payload.
     */
    public void requestPasswordReset(PasswordResetRequest request) {
        findUserByEmail(request.email()).ifPresentOrElse(user -> {
            String resetToken = UUID.randomUUID().toString();
            UUID tenantId = (request.tenantId() != null)
                    ? request.tenantId()
                    : (user.tenantId() != null ? user.tenantId() : PLATFORM_FALLBACK_TENANT_ID);

            log.info("Publishing Password Reset NotificationEvent for user [{}] under tenant [{}]", user.email(), tenantId);

            NotificationEvent event = NotificationEvent.of(
                    tenantId,
                    user.id(),
                    null,
                    NotificationType.PASSWORD_RESET,
                    "Password Reset Request - BookaBeeka",
                    user.email(),
                    "To reset your password, visit: https://bookabeeka.com/reset-password?token=" + resetToken,
                    Map.of(
                            "resetToken", resetToken,
                            "resetUrl", "https://bookabeeka.com/reset-password?token=" + resetToken,
                            "customerName", user.email(),
                            "expiryMinutes", 15
                    )
            );
            eventPublisher.publishEvent(event);

        }, () -> log.warn("Password reset requested for non-existent email [{}]. Silently ignored per OWASP.", request.email()));
    }

    private Optional<AuthUserDTO> findUserByEmail(String email) {
        return customerPort.findCustomerByEmail(email)
                .or(() -> hotelAdminPort.findAdminByEmail(email))
                .or(() -> ownerPort.findOwnerByEmail(email))
                .or(() -> superAdminPort.findSuperAdminByEmail(email));
    }

    private LoginResponse verifyAndIssueToken(AuthUserDTO user, String rawPassword) {
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }
        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.role());
    }
}