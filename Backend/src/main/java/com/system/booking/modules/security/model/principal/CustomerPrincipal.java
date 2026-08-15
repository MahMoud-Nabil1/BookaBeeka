package com.system.booking.modules.security.model.principal;

import java.util.UUID;

/**
 * Immutable security principal representing an authenticated Customer.
 *
 * <p>This record is set as the {@code principal} in the Spring Security
 * {@code UsernamePasswordAuthenticationToken} for customer requests.</p>
 *
 * <p><b>Isolation Guarantee:</b> Unlike {@link StaffPrincipal}, this principal
 * carries <b>no tenant affiliation</b> — no tenantId, no branchId, no role.
 * Customers have global access (not scoped to any tenant) and their tokens
 * never trigger TenantContext initialization.</p>
 */
public record CustomerPrincipal(
        UUID id,
        String email
) {}