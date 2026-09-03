package com.system.booking.modules.inventory.internal.dto;

public record UpdateAmenityRequest(
    String name,
    String description,
    Boolean isActive
) {}
