package com.system.booking.modules.tenant.api.dto;

import java.util.Map;

/**
 * Request payload for updating a tenant's profile and settings.
 * All fields are optional — only non-null values will be applied.
 */
public record UpdateTenantRequestDto(
        String name,
        Map<String, Object> settings,
        String timezone,
        String currency
) {}
