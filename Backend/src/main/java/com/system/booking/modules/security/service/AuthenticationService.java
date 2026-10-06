package com.system.booking.modules.security.service;

import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.dto.request.LoginRequest;
import com.system.booking.modules.security.dto.request.OtpRequest;
import com.system.booking.modules.security.dto.request.OtpVerificationRequest;
import com.system.booking.modules.security.dto.request.PasswordResetRequest;
import com.system.booking.modules.security.dto.request.ResetPasswordRequest;
import com.system.booking.modules.security.dto.response.LoginResponse;
import com.system.booking.modules.security.dto.response.OtpVerificationResponse;
import com.system.booking.modules.security.port.in.CustomerAuthPort;
import com.system.booking.modules.security.port.in.HotelAdminAuthPort;
import com.system.booking.modules.security.port.in.OwnerAuthPort;
import com.system.booking.modules.security.port.in.SuperAdminAuthPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    private final SuperAdminAuthPort superAdminPort;
    private final OwnerAuthPort ownerPort;
    private final HotelAdminAuthPort hotelAdminPort;
    private final CustomerAuthPort customerPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityTokenStore tokenStore;

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

    /**
     * Unified login method for OWNER and ADMIN roles only.
     * 
     * <p>Attempts authentication against Owner and Admin user types in sequence:
     * <ol>
     *   <li>Owner - Hotel owner with tenant-scoped access</li>
     *   <li>Admin - Branch admin with tenant and branch-scoped access</li>
     * </ol>
     * </p>
     * 
     * <p><strong>Security Enforcement:</strong> This method explicitly REJECTS SuperAdmin credentials
     * to enforce proper role separation. SuperAdmins must use the dedicated
     * {@code /api/auth/super-admin/login} endpoint.</p>
     * 
     * <p>Returns a JWT token for the first successful authentication match.
     * Throws BadCredentialsException if credentials don't match any allowed role.</p>
     * 
     * @param request Login credentials (email and password)
     * @return LoginResponse containing JWT token and user role
     * @throws BadCredentialsException if authentication fails for all allowed roles or if SuperAdmin credentials are provided
     */
    public LoginResponse loginOwnerOrAdmin(LoginRequest request) {
        // SECURITY: Explicitly reject SuperAdmin credentials - they must use /super-admin/login
        var superAdmin = superAdminPort.findSuperAdminByEmail(request.email());
        if (superAdmin.isPresent()) {
            log.warn("SuperAdmin attempted to login via /owner/login endpoint. Email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Try Owner
        var owner = ownerPort.findOwnerByEmail(request.email());
        if (owner.isPresent()) {
            try {
                return verifyAndIssueToken(owner.get(), request.password());
            } catch (BadCredentialsException e) {
                // Wrong password for this owner, continue to next check
            }
        }

        // Try Admin
        var admin = hotelAdminPort.findAdminByEmail(request.email());
        if (admin.isPresent()) {
            try {
                return verifyAndIssueToken(admin.get(), request.password());
            } catch (BadCredentialsException e) {
                // Wrong password for this admin, throw final exception
            }
        }

        // No user found with this email in Owner or Admin tables
        throw new BadCredentialsException("Invalid email or password");
    }

    public LoginResponse loginCustomer(LoginRequest request) {
        var user = customerPort.findCustomerByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    /**
     * Generates a 6-digit OTP code, saves it to cache with an expiration time,
     * and dispatches a NotificationEvent of type OTP_REQUESTED.
     *
     * @param request The OTP request payload containing recipient email and optional tenantId.
     */
    public void requestOtp(OtpRequest request) {
        findUserByEmail(request.email()).ifPresentOrElse(user -> {
            String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            UUID tenantId = (request.tenantId() != null)
                    ? request.tenantId()
                    : (user.tenantId() != null ? user.tenantId() : PLATFORM_FALLBACK_TENANT_ID);

            UUID effectiveTenant = (user.tenantId() != null) ? user.tenantId() : request.tenantId();
            tokenStore.storeOtp(user.email(), effectiveTenant, user.id(), otpCode);

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
     * Verifies a submitted OTP code against the cached token store.
     * Enforces attempt thresholds, timing-attack-safe comparison, and strict tenant isolation.
     *
     * @param request The OTP verification payload.
     * @return Verification response.
     */
    public OtpVerificationResponse verifyOtp(OtpVerificationRequest request) {
        // Resolve the same effectiveTenant used during storeOtp to ensure the cache key matches.
        // storeOtp uses: user.tenantId() != null ? user.tenantId() : request.tenantId()
        UUID verifyTenantId = findUserByEmail(request.email())
                .map(u -> u.tenantId() != null ? u.tenantId() : request.tenantId())
                .orElse(request.tenantId());

        var entry = tokenStore.verifyAndConsumeOtp(request.email(), verifyTenantId, request.otpCode());

        // Establish tenant context if the user is tenant-scoped
        if (entry.tenantId() != null && !PLATFORM_FALLBACK_TENANT_ID.equals(entry.tenantId())) {
            TenantContextHolder.setContext(new TenantContext(entry.tenantId()));
        }

        try {
            log.info("Successfully verified OTP for user [{}] under tenant [{}]", request.email(), entry.tenantId());
            return OtpVerificationResponse.success("OTP verified successfully");
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Generates a secure password reset token, saves it to cache with an expiration time,
     * and dispatches a NotificationEvent of type PASSWORD_RESET.
     *
     * @param request The password reset request payload.
     */
    public void requestPasswordReset(PasswordResetRequest request) {
        findUserByEmail(request.email()).ifPresentOrElse(user -> {
            String resetToken = UUID.randomUUID().toString();
            UUID tenantId = (request.tenantId() != null)
                    ? request.tenantId()
                    : (user.tenantId() != null ? user.tenantId() : PLATFORM_FALLBACK_TENANT_ID);

            UUID effectiveTenant = (user.tenantId() != null) ? user.tenantId() : request.tenantId();
            tokenStore.storePasswordResetToken(resetToken, user.id(), user.role(), user.email(), effectiveTenant);

            log.info("Publishing Password Reset NotificationEvent for user [{}] under tenant [{}]", user.email(), tenantId);

            String resetUrl = appBaseUrl + "/reset-password?token=" + resetToken;
            NotificationEvent event = NotificationEvent.of(
                    tenantId,
                    user.id(),
                    null,
                    NotificationType.PASSWORD_RESET,
                    "Password Reset Request - BookaBeeka",
                    user.email(),
                    "To reset your password, visit: " + resetUrl,
                    Map.of(
                            "resetToken", resetToken,
                            "resetUrl", resetUrl,
                            "customerName", user.email(),
                            "expiryMinutes", 15
                    )
            );
            eventPublisher.publishEvent(event);

        }, () -> log.warn("Password reset requested for non-existent email [{}]. Silently ignored per OWASP.", request.email()));
    }

    /**
     * Validates the password reset token, encodes the new password using BCryptPasswordEncoder,
     * and updates the user's credentials in the database scoped by role and tenant.
     *
     * @param request The reset password payload containing the token and new password.
     */
    public void resetPassword(ResetPasswordRequest request) {
        var entry = tokenStore.consumePasswordResetToken(request.token());

        // Establish tenant context if the user belongs to a tenant
        if (entry.tenantId() != null && !PLATFORM_FALLBACK_TENANT_ID.equals(entry.tenantId())) {
            TenantContextHolder.setContext(new TenantContext(entry.tenantId()));
        }

        try {
            String encodedPassword = passwordEncoder.encode(request.newPassword());

            switch (entry.role()) {
                case "CUSTOMER" -> customerPort.updatePassword(entry.userId(), encodedPassword);
                case "ADMIN" -> hotelAdminPort.updatePassword(entry.userId(), encodedPassword);
                case "OWNER" -> ownerPort.updatePassword(entry.userId(), encodedPassword);
                case "SUPER_ADMIN" -> superAdminPort.updatePassword(entry.userId(), encodedPassword);
                default -> throw new IllegalStateException("Unsupported user role: " + entry.role());
            }

            log.info("Successfully reset password for user [{}] with role [{}]", entry.email(), entry.role());
        } finally {
            TenantContextHolder.clear();
        }
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
        
        // Map roles to userType for frontend routing
        // SUPER_ADMIN, OWNER, ADMIN, and STAFF all map to "STAFF" userType
        // CUSTOMER maps to "CUSTOMER" userType
        String userType = "CUSTOMER".equals(user.role()) ? "CUSTOMER" : "STAFF";
        
        return new LoginResponse(token, userType);
    }
}
