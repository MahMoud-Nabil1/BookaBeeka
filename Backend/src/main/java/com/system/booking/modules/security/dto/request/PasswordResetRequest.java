package com.system.booking.modules.security.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request payload to trigger a password reset email notification.
 */
public record PasswordResetRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        UUID tenantId
) {}
