package com.system.booking.modules.inventory.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAmenityRequest(
    @NotBlank String name,
    String description
) {}
