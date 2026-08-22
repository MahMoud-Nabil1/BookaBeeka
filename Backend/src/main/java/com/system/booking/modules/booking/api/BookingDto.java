package com.system.booking.modules.booking.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// full booking representation for reads
public record BookingDto(
        UUID bookingId,
        UUID tenantId,
        UUID customerId,
        UUID resourceId,
        UUID serviceOfferingId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        BigDecimal totalAmount,
        String currency,
        String cancellationReason,
        Integer version,
        OffsetDateTime createdAt
) {}
