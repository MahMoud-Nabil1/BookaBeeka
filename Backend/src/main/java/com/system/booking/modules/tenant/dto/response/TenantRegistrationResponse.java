package com.system.booking.modules.tenant.dto.response;

import java.util.UUID;

/**
 * Response returned after a successful tenant onboarding.
 *
 * <p>Contains the identifiers of all three entities created atomically during
 * the registration process, plus the owner's email for confirmation.</p>
 *
 * <p>The SUPER_ADMIN who initiated the onboarding can use these IDs to verify
 * the tenant was created correctly and to communicate credentials to the
 * business owner.</p>
 */
public record TenantRegistrationResponse(
        UUID tenantId,
        UUID branchId,
        String ownerEmail,
        String message
) {}
