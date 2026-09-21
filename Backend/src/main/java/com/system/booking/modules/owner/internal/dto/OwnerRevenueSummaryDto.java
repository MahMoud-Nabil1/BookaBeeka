package com.system.booking.modules.owner.internal.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OwnerRevenueSummaryDto(
        UUID tenantId,
        String tenantName,
        BigDecimal overallRevenue,
        BigDecimal walletBalance,
        String currency,
        long totalCompletedBookings
) {}