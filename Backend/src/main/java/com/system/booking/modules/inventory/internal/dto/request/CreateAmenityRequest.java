package com.system.booking.modules.inventory.internal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAmenityRequest(
        @NotBlank(message = "Amenity name is required") String name,
        String description,
        String iconUrl
) {}