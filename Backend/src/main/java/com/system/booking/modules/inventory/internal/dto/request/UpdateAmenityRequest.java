package com.system.booking.modules.inventory.internal.dto.request;

public record UpdateAmenityRequest(
        String name,
        String description,
        String iconUrl,
        Boolean isActive
) {}