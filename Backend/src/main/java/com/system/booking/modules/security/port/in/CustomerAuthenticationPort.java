package com.system.booking.modules.security.port.in;

import com.system.booking.modules.security.dto.CustomerAuthDTO;
import java.util.Optional;

/**
 * Inbound port (Hexagonal Architecture) for Customer authentication data retrieval.
 *
 * <p><b>Defined by:</b> Security module (the client/consumer).<br>
 * <b>Implemented by:</b> Customer module ({@code CustomerSecurityAdapter}) — the provider.</p>
 *
 * <p>This port guarantees that the Security module never directly accesses
 * {@code CustomerRepository}. The Customer module provides the adapter
 * implementation that maps its internal entity to the security DTO.</p>
 *
 * <p><b>Isolation Guarantee:</b> Customer authentication is completely isolated
 * from the Staff/Tenant ecosystem — different port, different DTO, different
 * JWT claims, and no TenantContext involvement.</p>
 */
public interface CustomerAuthenticationPort {

    /**
     * Looks up a customer by email for authentication.
     *
     * @param email the customer's email address
     * @return an Optional containing the customer authentication data, or empty if not found
     */
    Optional<CustomerAuthDTO> findCustomerByEmail(String email);
}