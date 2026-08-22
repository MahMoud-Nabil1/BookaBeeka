package com.system.booking.modules.booking.api;

import java.time.OffsetDateTime;
import java.util.UUID;

// what the client sends to create a booking
public record CreateBookingRequestDto(
        UUID tenantId,
        UUID resourceId,
        UUID serviceOfferingId,
        OffsetDateTime start,
        OffsetDateTime end
) {}
