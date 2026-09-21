package com.system.booking.modules.owner.internal.dto;

import java.math.BigDecimal;

public record OwnerDashboardResponse(
        OwnerTenantSummaryDto tenant,
        BigDecimal overallRevenue,
        BigDecimal walletBalance,
        String currency,
        long totalBookings,
        long totalAdmins
) {}