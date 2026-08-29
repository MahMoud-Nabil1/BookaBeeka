package com.system.booking.modules.owner.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tenant summary as seen by the Owner — includes operational KPIs.
 */
public record OwnerTenantSummaryDto(
        UUID id,
        String name,
        String subdomain,
        String status,
        String timezone,
        String currency,
        LocalDateTime createdAt,
        long branchCount,
        BigDecimal totalRevenue
) {}
