package com.system.booking.modules.tenant.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Standard response DTO representing a Tenant's profile and settings.
 * Returned by all tenant-facing endpoints.
 */
public record TenantDto(
        UUID id,
        String name,
        String subdomain,
        String status,
        Map<String, Object> settings,
        String timezone,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
