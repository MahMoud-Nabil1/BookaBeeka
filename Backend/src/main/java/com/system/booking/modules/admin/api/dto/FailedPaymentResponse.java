package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** A FAILED payment record for the SuperAdmin failed-payments feed. */
public record FailedPaymentResponse(
        UUID paymentId,
        UUID bookingId,
        UUID tenantId,
        BigDecimal amount,
        String currency,
        String failureReason,
        LocalDateTime createdAt
) {}
