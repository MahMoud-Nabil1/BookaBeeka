package com.system.booking.modules.availability.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomSearchRequest(
        UUID hotelId,
        LocalDate checkIn,
        LocalDate checkOut,
        String roomType,
        String bedType,
        Integer minCapacity,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<UUID> amenities
) {}
