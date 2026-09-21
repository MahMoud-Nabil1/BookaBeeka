package com.system.booking.modules.availability.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RoomInfo(
        UUID id,
        String roomType,
        Integer capacity,
        String bedType,
        List<String> amenities,
        Map<String, Object> specs
) {}
