package com.system.booking.modules.inventory.internal.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AmenityResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        String iconUrl,
        Boolean isActive,
        LocalDateTime createdAt
) {}