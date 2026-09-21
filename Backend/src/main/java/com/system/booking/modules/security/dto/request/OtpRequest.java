package com.system.booking.modules.security.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request payload to trigger an OTP generation and notification dispatch.
 */
public record OtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        UUID tenantId
) {}
