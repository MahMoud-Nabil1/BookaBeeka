package com.system.booking.modules.inventory.internal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RoomTypeCreateRequest(
        @NotBlank(message = "Room type name is required") String name,
        String description,
        @NotNull(message = "Capacity is required") @Positive(message = "Capacity must be positive") Integer capacity,
        @NotNull(message = "Base price per night is required") @Positive(message = "Base price must be positive") BigDecimal basePricePerNight
) {}