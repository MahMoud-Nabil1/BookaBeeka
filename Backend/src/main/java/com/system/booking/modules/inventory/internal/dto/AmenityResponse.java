package com.system.booking.modules.inventory.internal.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AmenityResponse(
    UUID id,
    UUID tenantId,
    String name,
    String description,
    Boolean isActive,
    LocalDateTime createdAt
) {}
