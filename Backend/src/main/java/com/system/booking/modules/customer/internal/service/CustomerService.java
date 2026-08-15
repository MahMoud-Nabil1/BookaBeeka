package com.system.booking.modules.customer.internal.service;

import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service handling Customer operations (e.g., registration, profile updates).
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * The PasswordEncoder is provided by the Security module (PasswordEncoderConfig).
     * We inject it here to hash the customer's password during registration.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new customer in the system.
     *
     * <p>Validates that the email is globally unique, hashes the plaintext password,
     * and persists the new customer entity.</p>
     *
     * @param request the validated registration data
     * @throws IllegalArgumentException if the email is already registered
     */
    @Transactional
    public void registerCustomer(CustomerRegisterRequest request) {
        // Step 1: Ensure the email is globally unique
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Step 2: Build the new Customer entity
        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        // Step 3: Hash the plaintext password securely before saving
        String hashedPass = passwordEncoder.encode(request.password());
        customer.setPasswordHash(hashedPass);

        // Step 4: Persist to the database
        customerRepository.save(customer);
    }
}