package com.system.booking.modules.security.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload for Staff and Admin authentication.
 *
 * <p>Submitted to {@code POST /api/auth/staff/login}. The email is used to look up
 * the staff member via the {@code StaffAuthenticationPort}, and the password is
 * verified against the stored BCrypt hash.</p>
 */
public record StaffLoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}