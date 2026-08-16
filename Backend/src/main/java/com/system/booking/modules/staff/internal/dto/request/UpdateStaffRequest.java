package com.system.booking.modules.staff.internal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing staff member within the current tenant.
 *
 * <p>Submitted to {@code PUT /api/staff/{id}} by OWNER or ADMIN users.
 * Updates are scoped to the caller's tenant — the target staff member must
 * belong to the same tenant as the authenticated caller.</p>
 *
 * <p><b>Immutable Fields:</b> Email and password are intentionally excluded
 * from this DTO. Email is an immutable login credential, and password changes
 * should be handled through a dedicated password reset/change flow.</p>
 */
public record UpdateStaffRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        /** Phone number — optional, can be null to clear. */
        String phone,

        /**
         * Role to assign (must be "ADMIN" or "STAFF").
         * OWNER role cannot be reassigned through this endpoint.
         */
        @NotBlank(message = "Role is required")
        String role,

        /** Account status flag — set to false to deactivate the staff member. */
        @NotNull(message = "Active status is required")
        Boolean isActive
) {}
