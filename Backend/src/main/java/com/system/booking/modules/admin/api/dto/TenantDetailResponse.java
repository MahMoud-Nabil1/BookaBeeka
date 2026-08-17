package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Full tenant detail returned by {@code GET /api/admin/super/tenants/{id}}. */
public record TenantDetailResponse(
        UUID id,
        String name,
        String subdomain,
        String status,
        String currency,
        String timezone,

        // Financial snapshot
        BigDecimal revenueBalance,          // TenantWallet.balance

        // Booking stats for this tenant
        long totalBookings,
        long confirmedBookings,
        long completedBookings,
        long cancelledBookings,

        LocalDateTime createdAt
) {}
