package com.system.booking.modules.security.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload for Customer authentication.
 *
 * <p>Submitted to {@code POST /api/auth/customer/login}. The email is used to look up
 * the customer via the {@code CustomerAuthenticationPort}, and the password is
 * verified against the stored BCrypt hash.</p>
 *
 * <p><b>Isolation Note:</b> This endpoint and its processing pipeline are completely
 * separate from the Staff login flow — different port, different principal, different
 * JWT claims structure.</p>
 */
public record CustomerLoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}