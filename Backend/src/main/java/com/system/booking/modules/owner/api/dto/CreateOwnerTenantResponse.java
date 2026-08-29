package com.system.booking.modules.owner.api.dto;

import java.util.UUID;

/**
 * Response returned after an Owner successfully creates a new Tenant.
 */
public record CreateOwnerTenantResponse(
        UUID tenantId,
        String tenantName,
        String subdomain,
        UUID defaultBranchId,
        String message
) {}
