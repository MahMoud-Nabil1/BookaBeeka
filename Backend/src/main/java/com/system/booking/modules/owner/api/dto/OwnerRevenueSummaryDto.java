package com.system.booking.modules.owner.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Deep-dive revenue report for the Owner — overall tenant revenue
 * plus branch-by-branch financial breakdown.
 */
public record OwnerRevenueSummaryDto(
        UUID tenantId,
        String tenantName,
        BigDecimal overallRevenue,
        BigDecimal walletBalance,
        String currency,
        long totalCompletedBookings,
        List<BranchRevenueBreakdownDto> branchBreakdown
) {}
