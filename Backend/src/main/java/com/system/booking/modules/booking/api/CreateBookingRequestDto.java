package com.system.booking.modules.booking.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// what the client sends to create a booking — hotel-specialized
public record CreateBookingRequestDto(
        UUID tenantId,
        UUID roomId,           // the room being booked (maps to resource_id in DB)
        UUID serviceOfferingId,
        OffsetDateTime start,
        OffsetDateTime end,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer numberOfRooms,          // defaults to 1 if null
        String specialRequests,         // optional guest requests
        Map<String, Object> metadata    // optional extra data
) {}
