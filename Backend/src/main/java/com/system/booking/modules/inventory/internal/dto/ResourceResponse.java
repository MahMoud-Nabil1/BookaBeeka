package com.system.booking.modules.inventory.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ResourceResponse(
    UUID id,
    UUID tenantId,
    UUID branchId,
    String name,
    String resourceType,
    Integer capacity,
    Map<String, Object> specs,
    Boolean isActive,
    Boolean isBookable,
    BigDecimal pricePerNight,
    String currency,
    LocalDateTime createdAt
) {}
