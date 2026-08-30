package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;

/**
 * Top-level platform KPI snapshot returned by {@code GET /api/admin/super/stats}.
 * All counts are platform-wide (across all tenants).
 */
public record PlatformStatsResponse(

        // --- Tenants ---
        long totalTenants,
        long activeTenants,
        long suspendedTenants,
        long bannedTenants,

        // --- Customers ---
        long totalCustomers,
        long bannedCustomers,

        // --- Staff ---
        long totalStaff,

        // --- Bookings ---
        long totalBookings,
        long confirmedBookings,
        long completedBookings,
        long cancelledBookings,
        long stuckBookings,         // PENDING_PAYMENT > 30 min

        // --- Payments ---
        long failedPaymentsCount,

        // --- Financials ---
        BigDecimal platformRevenue,       // sum of all TenantWallet balances
        BigDecimal moneyInCirculation     // sum of all CustomerWallet balances
) {}
