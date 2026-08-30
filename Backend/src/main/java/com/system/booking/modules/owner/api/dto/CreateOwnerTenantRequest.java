package com.system.booking.modules.owner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for an Owner creating a new Tenant with a default branch.
 *
 * <p>The Owner is automatically provisioned as the OWNER staff member
 * in the newly created tenant.</p>
 */
public record CreateOwnerTenantRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(max = 255, message = "Tenant name must not exceed 255 characters")
        String tenantName,

        @NotBlank(message = "Subdomain is required")
        @Size(max = 100, message = "Subdomain must not exceed 100 characters")
        String subdomain,

        @NotBlank(message = "Branch name is required")
        @Size(max = 255, message = "Branch name must not exceed 255 characters")
        String branchName,

        @NotBlank(message = "Branch address is required")
        @Size(max = 500, message = "Branch address must not exceed 500 characters")
        String branchAddress,

        /** Timezone — defaults to "UTC" if not provided. */
        String timezone,

        /** Currency — defaults to "USD" if not provided. */
        String currency
) {}
