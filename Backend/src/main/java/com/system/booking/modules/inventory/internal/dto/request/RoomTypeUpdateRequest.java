package com.system.booking.modules.inventory.internal.dto.request;

import java.math.BigDecimal;

public record RoomTypeUpdateRequest(
        String name,
        String description,
        Integer capacity,
        BigDecimal basePricePerNight,
        Boolean isActive
) {}