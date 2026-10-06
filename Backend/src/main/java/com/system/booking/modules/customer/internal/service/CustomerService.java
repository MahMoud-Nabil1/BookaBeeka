package com.system.booking.modules.customer.internal.service;

import com.system.booking.modules.customer.internal.dto.CustomerProfileResponse;
import com.system.booking.modules.customer.internal.dto.CustomerProfileUpdateRequest;
import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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

    /**
     * Retrieves the profile of a customer by their ID.
     *
     * @param customerId the UUID of the customer
     * @return the customer profile response
     * @throws IllegalArgumentException if the customer is not found
     */
    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        return CustomerProfileResponse.builder()
                .id(customer.getId().toString())
                .email(customer.getEmail())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .createdAt(customer.getCreatedAt() != null ? customer.getCreatedAt() : LocalDateTime.now())
                .updatedAt(customer.getUpdatedAt() != null ? customer.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    /**
     * Updates the profile of a customer.
     *
     * @param customerId the UUID of the customer
     * @param request the validated update request
     * @return the updated customer profile response
     * @throws IllegalArgumentException if the customer is not found or email is already taken
     */
    @Transactional
    public CustomerProfileResponse updateCustomerProfile(UUID customerId, CustomerProfileUpdateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Check if email is being changed and if the new email is already taken
        if (!customer.getEmail().equals(request.getEmail()) && 
            customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Update customer fields
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        // Save and return updated profile
        Customer updatedCustomer = customerRepository.save(customer);

        return CustomerProfileResponse.builder()
                .id(updatedCustomer.getId().toString())
                .email(updatedCustomer.getEmail())
                .firstName(updatedCustomer.getFirstName())
                .lastName(updatedCustomer.getLastName())
                .phone(updatedCustomer.getPhone())
                .createdAt(updatedCustomer.getCreatedAt())
                .updatedAt(updatedCustomer.getUpdatedAt())
                .build();
    }

    /**
     * Changes the authenticated customer's password.
     *
     * @param customerId the UUID of the customer
     * @param request the validated change password request containing current and new password
     * @throws IllegalArgumentException if customer not found, current password incorrect, or new password same as current
     */
    @Transactional
    public void changePassword(UUID customerId, com.system.booking.modules.customer.internal.dto.CustomerChangePasswordRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (!passwordEncoder.matches(request.currentPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password");
        }

        customer.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        customerRepository.save(customer);
    }
}