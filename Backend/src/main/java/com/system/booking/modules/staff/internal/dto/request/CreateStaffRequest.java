package com.system.booking.modules.staff.internal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for creating a new staff member within the current tenant.
 *
 * <p>Submitted to {@code POST /api/staff} by OWNER or ADMIN users.
 * The staff member is automatically scoped to the caller's tenant — the
 * {@code tenantId} is never provided by the client; it is extracted from
 * the authenticated {@code StaffPrincipal}.</p>
 *
 * <p><b>Role Constraint:</b> Only ADMIN and STAFF roles can be assigned via
 * this endpoint. OWNER creation is restricted to the tenant onboarding flow
 * ({@code POST /api/tenants/register}), and SUPER_ADMIN is a platform-level
 * role that cannot be assigned through regular staff management.</p>
 */
public record CreateStaffRequest(

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

        /** Phone number — optional during staff creation. */
        String phone,

        /**
         * Role to assign (must be "ADMIN" or "STAFF").
         * OWNER creation is restricted to the tenant onboarding flow.
         */
        @NotBlank(message = "Role is required")
        String role
) {}
