package com.system.booking.modules.tenant.port.out;

import java.util.UUID;

/**
 * Outbound port (Hexagonal Architecture) for provisioning staff members during
 * tenant onboarding.
 *
 * <p><b>Defined by:</b> Tenant module (the client/consumer).<br>
 * <b>Implemented by:</b> Staff module ({@code StaffProvisioningAdapter}) — the provider.</p>
 *
 * <p>This port exists because the Tenant module needs to create an OWNER staff member
 * as part of the atomic tenant registration flow, but it must not directly access
 * the Staff module's internal repository. The Staff module provides the implementation
 * that handles password hashing and entity persistence.</p>
 *
 * <p><b>Separation of Concerns:</b> This port is strictly for <b>write/creation</b>
 * operations. Read/authentication operations go through the separate
 * {@code StaffAuthenticationPort} defined in the Security module.</p>
 */
public interface StaffProvisioningPort {

    /**
     * Creates an OWNER staff member associated with the given tenant and branch.
     *
     * <p>The implementation must hash the plaintext password before persisting.
     * This method is called within the tenant registration transaction to ensure
     * atomicity — if owner creation fails, the entire tenant onboarding rolls back.</p>
     *
     * @param tenantId  the UUID of the newly created tenant
     * @param branchId  the UUID of the default branch
     * @param firstName the owner's first name
     * @param lastName  the owner's last name
     * @param email     the owner's email (used for login)
     * @param password  the owner's plaintext password (will be hashed by the implementation)
     * @param phone     the owner's phone number (optional, may be null)
     */
    void createOwnerStaff(
            UUID tenantId,
            UUID branchId,
            String firstName,
            String lastName,
            String email,
            String password,
            String phone
    );
}