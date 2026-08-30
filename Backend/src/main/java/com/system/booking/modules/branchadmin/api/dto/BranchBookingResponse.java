package com.system.booking.modules.branchadmin.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchBookingResponse(
        UUID bookingId,
        UUID customerId,
        UUID resourceId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime createdAt
) {
}
