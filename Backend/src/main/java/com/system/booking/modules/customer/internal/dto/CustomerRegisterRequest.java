package com.system.booking.modules.customer.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for customer self-registration.
 *
 * <p>Submitted to {@code POST /api/customers/register} (public endpoint).
 * The password is hashed via {@code PasswordEncoder} before persistence —
 * it is never stored or logged in plaintext.</p>
 */
public record CustomerRegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        /** Phone number — optional during registration. */
        String phone
) {}