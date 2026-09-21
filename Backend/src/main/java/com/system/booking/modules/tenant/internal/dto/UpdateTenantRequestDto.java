package com.system.booking.modules.tenant.internal.dto;

import java.util.Map;

/**
 * Request payload for updating a tenant's profile and settings.
 * All fields are optional — only non-null values will be applied.
 *
 * <p>Uses {@code hotelName} (not {@code name}) to stay consistent with
 * {@code OwnerRegisterRequest} and avoid silent null-deserialization bugs.</p>
 */
public record UpdateTenantRequestDto(
        String hotelName,
        Map<String, Object> settings,
        String timezone,
        String currency
) {}
