package com.system.booking.modules.owner.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed Admin profile as seen by the Owner.
 *
 * <p>Includes the Admin's personal details, assigned branch information,
 * account status, and timestamps. Returned by {@code GET /api/v1/owner/admins}.</p>
 */
public record OwnerAdminSummaryDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        UUID tenantId,
        UUID branchId,
        String branchName,
        String branchAddress,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
