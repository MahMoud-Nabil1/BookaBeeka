package com.system.booking.modules.inventory.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateServiceOfferingRequest(
    @NotNull UUID tenantId,
    @NotNull UUID branchId,
    @NotBlank String name,
    @NotNull BigDecimal price,
    @NotNull Integer durationMinutes,
    Integer bufferMinutes,
    Map<String, Object> customAttributes
) {}
