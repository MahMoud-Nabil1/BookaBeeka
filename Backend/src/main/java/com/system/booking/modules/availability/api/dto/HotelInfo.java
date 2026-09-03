package com.system.booking.modules.availability.api.dto;

import java.util.UUID;

public record HotelInfo(
        UUID id,
        String name,
        String subdomain
) {}
