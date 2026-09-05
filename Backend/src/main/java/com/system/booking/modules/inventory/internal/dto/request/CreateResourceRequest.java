package com.system.booking.modules.inventory.internal.dto.request;

import com.system.booking.modules.inventory.internal.entity.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateResourceRequest(
        @NotNull(message = "Branch ID is required") UUID branchId,
        UUID roomTypeId,
        @NotBlank(message = "Room name/label is required") String name,
        String roomNumber,
        Integer floor,
        RoomStatus status,
        String resourceType,
        Integer capacity,
        Map<String, Object> specs,
        BigDecimal pricePerNight,
        String currency
) {}