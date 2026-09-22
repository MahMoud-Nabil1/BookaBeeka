package com.system.booking.modules.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory cache store for ephemeral authentication tokens:
 * <ul>
 *   <li>One-Time Passwords (OTP) with rate-limiting and maximum failed attempt thresholds.</li>
 *   <li>Password Reset Tokens with strict single-use consumption and tenant isolation metadata.</li>
 * </ul>
 */
@Component
@Slf4j
public class SecurityTokenStore {

    public static final int MAX_OTP_ATTEMPTS = 5;
    public static final long DEFAULT_OTP_TTL_SECONDS = 600; // 10 minutes
    public static final long DEFAULT_RESET_TOKEN_TTL_SECONDS = 900; // 15 minutes

    public record OtpEntry(
            String code,
            UUID userId,
            UUID tenantId,
            Instant expiresAt,
            AtomicInteger attempts
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record PasswordResetEntry(
            String token,
            UUID userId,
            String role,
            String email,
            UUID tenantId,
            Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, PasswordResetEntry> resetTokenStore = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // OTP Operations
    // -------------------------------------------------------------------------

    public void storeOtp(String email, UUID tenantId, UUID userId, String otpCode) {
        storeOtp(email, tenantId, userId, otpCode, DEFAULT_OTP_TTL_SECONDS);
    }

    public void storeOtp(String email, UUID tenantId, UUID userId, String otpCode, long ttlSeconds) {
        String key = buildOtpKey(email, tenantId);
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        otpStore.put(key, new OtpEntry(otpCode, userId, tenantId, expiresAt, new AtomicInteger(0)));
        log.debug("Stored OTP for key [{}] expiring at [{}]", key, expiresAt);
    }

    public Optional<OtpEntry> getOtp(String email, UUID tenantId) {
        String key = buildOtpKey(email, tenantId);
        OtpEntry entry = otpStore.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            otpStore.remove(key);
            log.debug("OTP for key [{}] expired and was removed", key);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    /**
     * Verifies the submitted OTP code.
     * Uses constant-time comparison to prevent timing attacks and tracks attempts to prevent brute-forcing.
     * Consumes (removes) the OTP on successful verification.
     */
    public OtpEntry verifyAndConsumeOtp(String email, UUID tenantId, String submittedCode) {
        String key = buildOtpKey(email, tenantId);
        OtpEntry entry = otpStore.get(key);

        if (entry == null || entry.isExpired()) {
            otpStore.remove(key);
            throw new BadCredentialsException("Invalid or expired verification code");
        }

        int currentAttempts = entry.attempts().incrementAndGet();
        if (currentAttempts > MAX_OTP_ATTEMPTS) {
            otpStore.remove(key);
            log.warn("Exceeded max OTP verification attempts for key [{}]", key);
            throw new BadCredentialsException("Too many invalid attempts. Please request a new verification code.");
        }

        byte[] expected = entry.code().getBytes(StandardCharsets.UTF_8);
        byte[] actual = submittedCode.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expected, actual)) {
            log.warn("Invalid OTP attempt [{}/{}] for key [{}]", currentAttempts, MAX_OTP_ATTEMPTS, key);
            throw new BadCredentialsException("Invalid verification code");
        }

        // Successfully verified — remove immediately (single-use guarantee)
        otpStore.remove(key);
        return entry;
    }

    // -------------------------------------------------------------------------
    // Password Reset Operations
    // -------------------------------------------------------------------------

    public void storePasswordResetToken(String token, UUID userId, String role, String email, UUID tenantId) {
        storePasswordResetToken(token, userId, role, email, tenantId, DEFAULT_RESET_TOKEN_TTL_SECONDS);
    }

    public void storePasswordResetToken(String token, UUID userId, String role, String email, UUID tenantId, long ttlSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        resetTokenStore.put(token, new PasswordResetEntry(token, userId, role, email, tenantId, expiresAt));
        log.debug("Stored password reset token for user [{}] expiring at [{}]", email, expiresAt);
    }

    public Optional<PasswordResetEntry> getPasswordResetToken(String token) {
        PasswordResetEntry entry = resetTokenStore.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            resetTokenStore.remove(token);
            log.debug("Password reset token [{}] expired and was removed", token);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    /**
     * Validates and atomically consumes a password reset token (single-use guarantee).
     */
    public PasswordResetEntry consumePasswordResetToken(String token) {
        PasswordResetEntry entry = resetTokenStore.remove(token);
        if (entry == null || entry.isExpired()) {
            throw new BadCredentialsException("Invalid or expired password reset token");
        }
        return entry;
    }

    // -------------------------------------------------------------------------
    // Eviction & Cleanup
    // -------------------------------------------------------------------------

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void evictExpiredEntries() {
        otpStore.entrySet().removeIf(e -> e.getValue().isExpired());
        resetTokenStore.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public void clear() {
        otpStore.clear();
        resetTokenStore.clear();
    }

    private String buildOtpKey(String email, UUID tenantId) {
        String normalizedEmail = email.trim().toLowerCase();
        return (tenantId != null) ? (tenantId + ":" + normalizedEmail) : normalizedEmail;
    }
}
