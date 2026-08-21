package com.system.booking.modules.security.model.principal;

import java.util.UUID;

/**
 * Immutable security principal representing an authenticated Staff or Admin user.
 *
 * <p>This record is set as the {@code principal} in the Spring Security
 * {@code UsernamePasswordAuthenticationToken} — both during login (by
 * {@code AuthenticationService}) and on every subsequent request (by
 * {@code JwtAuthenticationFilter} after JWT validation).</p>
 *
 * <h3>Fields:</h3>
 * <ul>
 *   <li>{@code id}        — Staff UUID (matches the JWT {@code sub} claim)</li>
 *   <li>{@code email}     — Staff email address</li>
 *   <li>{@code role}      — e.g., "OWNER", "ADMIN", "STAFF" (plain, without ROLE_ prefix)</li>
 *   <li>{@code tenantId}  — Tenant UUID for multi-tenant data isolation</li>
 *   <li>{@code branchId}  — Branch UUID for branch-level scoping</li>
 *   <li>{@code firstName} — Staff first name for display purposes</li>
 *   <li>{@code lastName}  — Staff last name for display purposes</li>
 * </ul>
 *
 * <p><b>Note:</b> During JWT-based requests, {@code email}, {@code firstName}, and
 * {@code lastName} may be {@code null} since these are not stored in the token
 * to keep it lightweight. They are only populated during the login flow.</p>
 */
public record StaffPrincipal(
        UUID id,
        String email,
        String role,
        UUID tenantId,
        UUID branchId
) {}