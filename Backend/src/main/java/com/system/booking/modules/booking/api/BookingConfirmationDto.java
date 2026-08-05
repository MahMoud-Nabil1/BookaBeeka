package com.system.booking.modules.booking.api;

import java.time.OffsetDateTime;
import java.util.UUID;

// returned right after a booking is created
public record BookingConfirmationDto(
        UUID bookingId,
        String status,
        UUID lockId,
        OffsetDateTime createdAt
) {}
