package com.system.booking.modules.security.port.in;

import com.system.booking.modules.security.dto.StaffAuthDTO;
import java.util.Optional;

/**
 * Inbound port (Hexagonal Architecture) for Staff authentication data retrieval.
 *
 * <p><b>Defined by:</b> Security module (the client/consumer).<br>
 * <b>Implemented by:</b> Staff module ({@code StaffSecurityAdapter}) — the provider.</p>
 *
 * <p>This port exists to maintain strict module boundaries. The Security module
 * never directly accesses {@code StaffRepository} — it only knows this interface.
 * The Staff module provides the implementation that bridges to its internal data layer.</p>
 *
 * <p><b>Read-Only Contract:</b> This port is strictly for reading/authentication purposes.
 * Write operations (e.g., creating staff members) go through a separate port
 * ({@code StaffProvisioningPort}) defined in the Tenant module.</p>
 */
public interface StaffAuthenticationPort {

    /**
     * Looks up a staff member by email for authentication.
     *
     * @param email the staff member's email address
     * @return an Optional containing the staff authentication data, or empty if not found
     */
    Optional<StaffAuthDTO> findStaffByEmail(String email);
}