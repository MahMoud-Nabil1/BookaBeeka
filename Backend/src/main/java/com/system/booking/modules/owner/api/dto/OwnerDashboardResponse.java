package com.system.booking.modules.owner.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated Owner dashboard response containing tenant summary,
 * branch overviews, and high-level KPIs.
 */
public record OwnerDashboardResponse(
        OwnerTenantSummaryDto tenant,
        BigDecimal overallRevenue,
        BigDecimal walletBalance,
        String currency,
        long totalBranches,
        long activeBranches,
        long totalBookings,
        long totalAdmins,
        List<OwnerBranchSummaryDto> branches
) {}
