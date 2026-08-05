package com.system.booking.modules.booking.internal.event;

import java.time.OffsetDateTime;
import java.util.UUID;

// fired when payment goes through and booking is confirmed
public record BookingConfirmedEvent(
        UUID bookingId,
        UUID tenantId,
        UUID customerId,
        UUID resourceId,
        OffsetDateTime slotStart,
        OffsetDateTime slotEnd
) {}
