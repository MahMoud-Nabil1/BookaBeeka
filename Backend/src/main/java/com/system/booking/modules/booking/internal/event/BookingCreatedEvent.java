package com.system.booking.modules.booking.internal.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// fired after a new booking is persisted
public record BookingCreatedEvent(
        UUID bookingId,
        UUID tenantId,
        UUID customerId,
        UUID resourceId,
        BigDecimal totalAmount,
        OffsetDateTime createdAt
) {}
