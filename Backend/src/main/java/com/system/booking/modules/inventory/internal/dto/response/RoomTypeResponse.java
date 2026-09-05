package com.system.booking.modules.inventory.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        UUID tenantId,
        UUID branchId,
        String name,
        String description,
        Integer capacity,
        BigDecimal basePricePerNight,
        Boolean isActive,
        LocalDateTime createdAt
) {}