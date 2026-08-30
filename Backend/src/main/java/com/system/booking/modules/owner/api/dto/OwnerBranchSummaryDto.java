package com.system.booking.modules.owner.api.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Branch summary as seen by the Owner — includes status, staff count,
 * booking counts, and revenue metrics.
 */
public record OwnerBranchSummaryDto(
        UUID branchId,
        String branchName,
        String address,
        String status,
        Map<String, Object> settings,
        long staffCount,
        long totalBookings,
        long completedBookings,
        BigDecimal branchRevenue,
        Double revenuePercentage
) {}
