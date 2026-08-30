package com.system.booking.modules.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /api/admin/super/tenants/{id}/status}.
 * Valid status values: ACTIVE, SUSPENDED, BANNED.
 */
public record UpdateTenantStatusRequest(
        @NotBlank String status
) {}
