package com.system.booking.modules.customer.internal.service;

import com.system.booking.modules.customer.internal.dto.CustomerProfileResponse;
import com.system.booking.modules.customer.internal.dto.CustomerProfileUpdateRequest;
import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.security.service.SecurityTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Core business service handling Customer operations (e.g., registration, profile updates).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * The PasswordEncoder is provided by the Security module (PasswordEncoderConfig).
     * We inject it here to hash the customer's password during registration.
     */
    private final PasswordEncoder passwordEncoder;

    private final ApplicationEventPublisher eventPublisher;
    private final SecurityTokenStore tokenStore;

    /**
     * Registers a new customer and immediately dispatches an OTP verification email.
     *
     * <p>The OTP is generated and stored <em>within</em> the current transaction so it
     * is available when the {@code AFTER_COMMIT} event fires on the async listener.
     * The {@link NotificationEvent} is published via Spring's
     * {@link ApplicationEventPublisher} and handled by
     * {@code NotificationEventListener} after the transaction commits.</p>
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
        Customer saved = customerRepository.save(customer);

        // Step 5: Generate a 6-digit OTP and store it in the in-memory token store.
        // Customers are cross-tenant (no tenantId), so we use the platform fallback UUID
        // (all-zeros). AuthenticationService.verifyOtp() resolves the same key for
        // customers by falling back to the user's tenantId (null → fallback UUID).
        UUID platformTenantId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        tokenStore.storeOtp(saved.getEmail(), platformTenantId, saved.getId(), otpCode);

        log.info("OTP stored and NotificationEvent published for new customer [{}]", saved.getEmail());

        // Step 6: Publish the notification event.
        // NotificationEventListener picks this up AFTER this transaction commits,
        // runs async, and calls EmailSenderService → SMTP → Gmail.
        NotificationEvent event = NotificationEvent.of(
                platformTenantId,
                saved.getId(),
                null,           // bookingId: not applicable
                NotificationType.OTP_REQUESTED,
                "Verify your BookaBeeka account",
                saved.getEmail(),
                "Your one-time verification code is: " + otpCode,
                Map.of(
                        "otpCode", otpCode,
                        "customerName", saved.getFirstName(),
                        "expiryMinutes", 10
                )
        );
        eventPublisher.publishEvent(event);
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