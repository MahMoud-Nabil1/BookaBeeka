package com.system.booking.modules.customer.internal.service;

import com.system.booking.modules.security.dto.CustomerAuthDTO;
import com.system.booking.modules.security.port.in.CustomerAuthenticationPort;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter implementing the {@link CustomerAuthenticationPort} (defined in the Security module).
 *
 * <p><b>Responsibility:</b> Strictly handles <b>read-only</b> operations for Customer
 * authentication. This ensures the Security module can retrieve customer credentials
 * without being coupled to the internal {@code CustomerRepository}.</p>
 *
 * <p><b>Isolation Guarantee:</b> This adapter is completely separate from the Staff
 * authentication flow, ensuring that a Customer can never accidentally authenticate
 * as a Staff member or vice versa.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomerSecurityAdapter implements CustomerAuthenticationPort {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAuthDTO> findCustomerByEmail(String email) {
        // Find the customer entity and map it to the DTO expected by the Security module
        return customerRepository.findByEmail(email)
                .map(customer -> new CustomerAuthDTO(
                        customer.getId(), // Inherited from BaseEntity
                        customer.getEmail(),
                        customer.getPasswordHash()
                ));
    }
}