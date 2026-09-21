package com.system.booking.modules.inventory.internal.dto.request;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateServiceOfferingRequest(
        String name,
        String description,
        BigDecimal price,
        Integer durationMinutes,
        Integer bufferMinutes,
        Map<String, Object> customAttributes,
        Boolean isActive
) {}