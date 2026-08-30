package com.system.booking.modules.branchadmin.api.dto;

import java.math.BigDecimal;

public record BranchDashboardResponse(
        String branchName,
        long totalBookingsToday,
        BigDecimal revenueToday
) {
}
