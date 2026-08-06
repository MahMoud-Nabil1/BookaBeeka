package com.system.booking.modules.booking.internal.event;

import java.util.UUID;

// fired when a pending booking times out without payment
public record BookingExpiredEvent(
        UUID bookingId,
        UUID tenantId
) {}
