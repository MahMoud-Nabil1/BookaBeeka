package com.system.booking.modules.admin.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Row in the SuperAdmin tenant management table. */
public record TenantSummaryResponse(
        UUID id,
        String name,
        String subdomain,
        String status,
        String currency,
        String timezone,
        LocalDateTime createdAt
) {}
