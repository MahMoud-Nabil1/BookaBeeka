package com.system.booking.modules.inventory.internal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;

public record CreateServiceOfferingRequest(
        @NotBlank(message = "Service name is required") String name,
        String description,
        @NotNull(message = "Price is required") @PositiveOrZero(message = "Price cannot be negative") BigDecimal price,
        Integer durationMinutes,
        Integer bufferMinutes,
        Map<String, Object> customAttributes
) {}