package com.system.booking.modules.owner.internal.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OwnerAdminSummaryDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        UUID tenantId,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}