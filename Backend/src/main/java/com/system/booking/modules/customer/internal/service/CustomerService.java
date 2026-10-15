package com.system.booking.modules.customer.internal.service;

import com.system.booking.config.SystemTenantInitializer;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.customer.internal.dto.CustomerProfileResponse;
import com.system.booking.modules.customer.internal.dto.CustomerProfileUpdateRequest;
import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import com.system.booking.modules.payment.internal.repository.CustomerWalletRepository;
import com.system.booking.modules.payment.internal.repository.WalletTransactionRepository;
import com.system.booking.modules.security.service.SecurityTokenStore;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core business service handling Customer operations (e.g., registration, profile updates, account deletion).
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
    private final BookingRepository bookingRepository;
    private final CustomerWalletRepository customerWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

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
        UUID platformTenantId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // Step 1: Check whether the email already exists in the system
        Customer customer = customerRepository.findByEmail(request.email()).orElse(null);

        if (customer != null) {
            if (Boolean.TRUE.equals(customer.getIsActive())) {
                // Already verified — do not allow re-registration
                throw new IllegalArgumentException("Email is already registered");
            }

            // Existing but unverified account: the previous OTP may never have arrived.
            // Update the record with the latest submission details so the user can retry
            // without being blocked. The account stays inactive until OTP is verified.
            log.info("Re-registration attempt for unverified customer [{}] — refreshing OTP", request.email());
            customer.setFirstName(request.firstName());
            customer.setLastName(request.lastName());
            customer.setPhone(request.phone());
            customer.setPasswordHash(passwordEncoder.encode(request.password()));
            // isActive remains false — no upgrade without OTP verification
        } else {
            // Step 2: Brand-new account — build and persist the Customer entity
            customer = new Customer();
            customer.setFirstName(request.firstName());
            customer.setLastName(request.lastName());
            customer.setEmail(request.email());
            customer.setPhone(request.phone());
            customer.setPasswordHash(passwordEncoder.encode(request.password()));
            // Account is inactive until OTP email verification is completed
            customer.setIsActive(false);
        }

        // Step 3: Persist (insert for new, update for retry)
        Customer saved = customerRepository.save(customer);

        // Step 4: Generate a fresh 6-digit OTP and (re-)store it in the token store.
        // Customers are cross-tenant (no tenantId), so we use the platform fallback UUID
        // (all-zeros). AuthenticationService.verifyOtp() resolves the same key for
        // customers by falling back to the user's tenantId (null → fallback UUID).
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        tokenStore.storeOtp(saved.getEmail(), platformTenantId, saved.getId(), otpCode);

        log.info("OTP stored and NotificationEvent published for customer [{}]", saved.getEmail());

        // Step 5: Publish the notification event.
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

        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Customer account is deactivated");
        }

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

        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Customer account is deactivated");
        }

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

        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Customer account is deactivated");
        }

        if (!passwordEncoder.matches(request.currentPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password");
        }

        customer.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        customerRepository.save(customer);
    }

    /**
     * Deletes (deactivates and anonymizes) the authenticated customer's account safely.
     *
     * <p>Enforces safety guards:
     * <ul>
     *   <li>Rejects request if customer has active or upcoming reservations.</li>
     *   <li>Rejects request if customer has a positive wallet balance.</li>
     *   <li>Reassigns past bookings and reviews to {@link SystemTenantInitializer#ANONYMOUS_CUSTOMER_ID}
     *       so hotel revenue and occupancy metrics remain mathematically intact.</li>
     *   <li>Deletes ephemeral data (notifications, favorites).</li>
     *   <li>Cleans up 0-balance wallet and wallet transactions.</li>
     *   <li>Anonymizes personal information (PII) and disables the account ({@code isActive = false}).</li>
     *   <li>Marks the customer ID as deactivated in {@link SecurityTokenStore} to immediately invalidate
     *       any already-issued JWTs.</li>
     * </ul>
     *
     * @param customerId the UUID of the customer extracted from the authenticated principal
     */
    @Transactional
    public void deleteAccount(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Idempotency: already deactivated account exits cleanly
        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            log.info("Customer [{}] is already deactivated", customerId);
            return;
        }

        // Guard 1: Active or upcoming bookings
        if (bookingRepository != null) {
            LocalDate today = LocalDate.now();
            OffsetDateTime now = OffsetDateTime.now();
            List<Booking> activeBookings = bookingRepository.findActiveOrUpcomingBookingsByCustomer(customerId, today, now);
            if (!activeBookings.isEmpty()) {
                Booking b = activeBookings.get(0);
                LocalDate activeUntil = b.getCheckOut() != null ? b.getCheckOut() : (b.getEndTime() != null ? b.getEndTime().toLocalDate() : today);
                throw new IllegalStateException("Cannot delete account: you have an active or upcoming reservation until " + activeUntil + ". Please complete or cancel your booking first.");
            }
        }

        // Guard 2: Outstanding wallet balance
        if (customerWalletRepository != null) {
            customerWalletRepository.findByCustomerId(customerId).ifPresent(wallet -> {
                if (wallet.getBalance() != null && wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalStateException("Cannot delete account with an active wallet balance (" + wallet.getBalance() + " " + wallet.getCurrency() + "). Please withdraw or spend your balance before closing your account.");
                }
                if (walletTransactionRepository != null) {
                    walletTransactionRepository.deleteByWalletId(wallet.getId());
                }
                customerWalletRepository.delete(wallet);
            });
        }

        // Cleanup 1: Ephemeral notifications
        if (notificationRepository != null) {
            notificationRepository.deleteByCustomerId(customerId);
        }

        // Cleanup 2: Ephemeral favorites
        if (entityManager != null) {
            try {
                entityManager.createNativeQuery("DELETE FROM customer_favorite WHERE customer_id = :cId")
                        .setParameter("cId", customerId)
                        .executeUpdate();
            } catch (Exception ex) {
                log.debug("Customer favorite cleanup skipped or not applicable: {}", ex.getMessage());
            }

            // Ensure anonymous customer placeholder exists
            ensureAnonymousCustomerExists();

            // Historical Preservation: Reassign past bookings & reviews to platform anonymous user
            try {
                entityManager.createNativeQuery("UPDATE booking SET customer_id = :anonId WHERE customer_id = :cId")
                        .setParameter("anonId", SystemTenantInitializer.ANONYMOUS_CUSTOMER_ID)
                        .setParameter("cId", customerId)
                        .executeUpdate();
            } catch (Exception ex) {
                log.debug("Booking reassignment to anonymous customer skipped: {}", ex.getMessage());
            }

            try {
                entityManager.createNativeQuery("UPDATE review SET customer_id = :anonId WHERE customer_id = :cId")
                        .setParameter("anonId", SystemTenantInitializer.ANONYMOUS_CUSTOMER_ID)
                        .setParameter("cId", customerId)
                        .executeUpdate();
            } catch (Exception ex) {
                log.debug("Review reassignment to anonymous customer skipped: {}", ex.getMessage());
            }
        }

        // Deactivation & Anonymization
        customer.setIsActive(false);
        customer.setFirstName("Deleted");
        customer.setLastName("User");
        customer.setPhone(null);
        customer.setEmail("deleted-" + customer.getId() + "@deleted.bookabeeka.com");
        customer.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        customerRepository.save(customer);

        // Immediate session/token revocation
        if (tokenStore != null) {
            tokenStore.markUserDeactivated(customerId);
        }

        log.info("Customer account successfully deleted/deactivated: customerId={}", customerId);
    }

    private void ensureAnonymousCustomerExists() {
        if (entityManager != null) {
            try {
                entityManager.createNativeQuery("""
                    INSERT INTO customer (id, email, password_hash, first_name, last_name, is_active, banned, created_at, updated_at)
                    VALUES ('00000000-0000-0000-0000-000000000002', 'deleted-user@platform.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi50jvd77Gq5gD0eP/84.XfQW809nku', 'Deleted', 'Customer', FALSE, TRUE, NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING
                """).executeUpdate();
            } catch (Exception ex) {
                log.debug("ensureAnonymousCustomerExists skipped or handled: {}", ex.getMessage());
            }
        }
    }
}