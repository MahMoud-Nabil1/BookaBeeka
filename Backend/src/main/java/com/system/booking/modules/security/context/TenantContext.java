package com.system.booking.modules.security.context;

import java.util.UUID;

/**
 * Immutable value object holding the current tenant and branch scope for a request.
 *
 * <p>Stored in a {@link ThreadLocal} via {@link TenantContextHolder}, this record
 * provides tenant-aware modules (e.g., Booking, Inventory, Availability) with the
 * identifiers they need for data isolation filtering.</p>
 *
 * <p><b>Lifecycle:</b> Set by the {@code JwtAuthenticationFilter} when processing
 * a Staff/Admin JWT, and cleared after the request completes. Never set for
 * Customer requests — Customers have global access with no tenant scope.</p>
 */
public record TenantContext(
        UUID tenantId,
        UUID branchId
) {}