package com.system.booking.modules.booking.api;

import java.math.BigDecimal;
import java.util.UUID;

// what the client gets back after a cancellation
public record CancellationResultDto(
        UUID bookingId,
        BigDecimal refundAmount,
        int refundPercentage,
        String newStatus
) {}
