package com.system.booking.modules.booking.internal.event;

import java.math.BigDecimal;
import java.util.UUID;

// fired when a confirmed booking gets cancelled
public record BookingCancelledEvent(
        UUID bookingId,
        UUID tenantId,
        BigDecimal refundAmount,
        String reason
) {}
