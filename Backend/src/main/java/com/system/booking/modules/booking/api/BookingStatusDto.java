package com.system.booking.modules.booking.api;

import java.util.UUID;

// lightweight status check response
public record BookingStatusDto(
        UUID bookingId,
        String status,
        Integer version
) {}
