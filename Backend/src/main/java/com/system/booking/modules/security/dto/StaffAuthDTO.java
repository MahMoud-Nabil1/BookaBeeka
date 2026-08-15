package com.system.booking.modules.security.dto;

import java.util.UUID;

/**
 * Data Transfer Object carrying staff authentication data across the hexagonal boundary.
 *
 * <p>This record is populated by the {@code StaffSecurityAdapter} (in the Staff module)
 * and consumed by the {@code AuthenticationService} (in the Security module) during
 * the login flow. It deliberately contains <b>only</b> the fields needed for
 * credential verification and JWT token generation — no business logic leaks.</p>
 *
 * <p><b>Fields:</b></p>
 * <ul>
 *   <li>{@code id}           — Staff UUID, becomes the JWT {@code sub} claim</li>
 *   <li>{@code email}        — Used for credential lookup</li>
 *   <li>{@code passwordHash} — BCrypt hash for password verification</li>
 *   <li>{@code role}         — e.g., "OWNER", "ADMIN", "STAFF" — becomes the JWT {@code role} claim</li>
 *   <li>{@code tenantId}     — Tenant UUID — becomes the JWT {@code tenant_id} claim</li>
 *   <li>{@code branchId}     — Branch UUID — becomes the JWT {@code branch_id} claim</li>
 *   <li>{@code firstName}    — Staff first name</li>
 *   <li>{@code lastName}     — Staff last name</li>
 *   <li>{@code isActive}     — Account status flag; inactive accounts are rejected at login</li>
 * </ul>
 */
public record StaffAuthDTO(
        UUID id,
        String email,
        String passwordHash,
        String role,
        UUID tenantId,
        UUID branchId,
        String firstName,
        String lastName,
        boolean isActive
) {}