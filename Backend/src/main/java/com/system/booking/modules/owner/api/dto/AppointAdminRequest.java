package com.system.booking.modules.owner.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for an Owner appointing/creating a new Admin for a branch.
 *
 * <p>The Owner provides the Admin's credentials and assigns them to a specific
 * branch within their tenant. The {@code branchId} must belong to the Owner's
 * tenant — this is validated by the service layer.</p>
 */
public record AppointAdminRequest(

        @NotNull(message = "Branch ID is required")
        UUID branchId,

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

        /** Phone number — optional. */
        String phone
) {}
