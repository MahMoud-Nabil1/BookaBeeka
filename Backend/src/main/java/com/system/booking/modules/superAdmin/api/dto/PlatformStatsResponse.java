package com.system.booking.modules.superAdmin.api.dto;

import java.math.BigDecimal;

public record PlatformStatsResponse(

        // --- Tenants ---
        long totalTenants,
        long activeTenants,
        long suspendedTenants,
        long bannedTenants,

        // --- Customers ---
        long totalCustomers,
        long bannedCustomers,

        // --- Hotel Users (Platform-wide) ---
        long totalHotelUsers,

        // --- Bookings ---
        long totalBookings,
        long confirmedBookings,
        long completedBookings,
        long cancelledBookings,
        long stuckBookings,

        // --- Payments ---
        long failedPaymentsCount,

        // --- Financials ---
        BigDecimal platformRevenue,
        BigDecimal moneyInCirculation
) {}