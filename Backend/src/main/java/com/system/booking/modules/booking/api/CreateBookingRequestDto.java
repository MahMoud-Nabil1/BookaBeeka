package com.system.booking.modules.booking.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// what the client sends to create a booking
public record CreateBookingRequestDto(
        UUID tenantId,
        UUID customerId,
        UUID resourceId,
        OffsetDateTime start,
        OffsetDateTime end,
        BigDecimal totalAmount,
        String currency
) {}
