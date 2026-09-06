package com.system.booking.modules.inventory.internal.dto.request;

import com.system.booking.modules.inventory.internal.entity.RoomStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateResourceRequest(
        UUID roomTypeId,
        String name,
        String roomNumber,
        Integer floor,
        RoomStatus status,
        String resourceType,
        Integer capacity,
        Map<String, Object> specs,
        Boolean isActive,
        Boolean isBookable,
        BigDecimal pricePerNight,
        String currency
) {}