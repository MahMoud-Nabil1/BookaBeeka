package com.system.booking.modules.security.dto;

import java.util.UUID;

/**
 * Data Transfer Object carrying customer authentication data across the hexagonal boundary.
 *
 * <p>This record is populated by the {@code CustomerSecurityAdapter} (in the Customer module)
 * and consumed by the {@code AuthenticationService} (in the Security module) during
 * the customer login flow.</p>
 *
 * <p><b>Design Note:</b> Customer tokens are intentionally simple — no tenant affiliation,
 * no role hierarchy. This guarantees absolute isolation between the Customer ecosystem
 * and the Staff/Tenant ecosystem.</p>
 */
public record CustomerAuthDTO(
        UUID id,
        String email,
        String passwordHash
) {}