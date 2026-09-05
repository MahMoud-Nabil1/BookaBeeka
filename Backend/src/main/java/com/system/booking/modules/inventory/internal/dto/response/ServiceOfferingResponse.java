package com.system.booking.modules.inventory.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ServiceOfferingResponse(
        UUID id,
        UUID tenantId,
        UUID branchId,
        String name,
        String description,
        BigDecimal price,
        Integer durationMinutes,
        Integer bufferMinutes,
        Map<String, Object> customAttributes,
        Boolean isActive,
        LocalDateTime createdAt
) {}