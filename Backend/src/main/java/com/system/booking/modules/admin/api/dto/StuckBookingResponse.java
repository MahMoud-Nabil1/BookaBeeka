package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A booking stuck in PENDING_PAYMENT for longer than the detection threshold.
 * Signals a potential payment processing issue that needs investigation.
 */
public record StuckBookingResponse(
        UUID bookingId,
        UUID customerId,
        UUID tenantId,
        BigDecimal totalAmount,
        String currency,
        long minutesStuck,      // how long ago it was created
        LocalDateTime createdAt
) {}
