package com.system.booking.modules.superAdmin.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /api/admin/super/tenants/{id}/status}.
 * Valid status values: ACTIVE, SUSPENDED, BANNED.
 */
public record UpdateTenantStatusRequest(
        @NotBlank String status
) {}
