package com.system.booking.modules.security.model.principal;

import java.util.UUID;

/**
 * Immutable security principal representing an authenticated Hotel Management User
 * (SUPER_ADMIN, OWNER, or ADMIN).
 *
 * <p>Carries the identity, role, and tenant scoping without any branch dependency.</p>
 */
public record HotelUserPrincipal(
        UUID id,
        String email,
        String role,
        UUID tenantId
) {}