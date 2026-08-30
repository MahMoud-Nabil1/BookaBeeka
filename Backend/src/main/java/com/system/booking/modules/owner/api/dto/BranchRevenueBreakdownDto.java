package com.system.booking.modules.owner.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Individual branch financial metric within a revenue breakdown.
 */
public record BranchRevenueBreakdownDto(
        UUID branchId,
        String branchName,
        String status,
        BigDecimal revenue,
        long completedBookingsCount,
        long pendingBookingsCount,
        BigDecimal averageBookingValue,
        Double percentageOfTotalRevenue
) {}
