package com.system.booking.modules.inventory.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateResourceRequest(
    @NotNull UUID tenantId,
    @NotNull UUID branchId,
    @NotBlank String name,
    @NotBlank String resourceType,
    Integer capacity,
    Map<String, Object> specs,
    BigDecimal pricePerNight,
    String currency
) {}
