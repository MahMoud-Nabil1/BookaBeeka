package com.system.booking.modules.customer.internal.repository;

import com.system.booking.modules.customer.internal.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Customer} entity persistence.
 *
 * <p><b>No Tenant Scoping:</b> Customers are global platform users — they
 * exist outside the tenant ecosystem and can book with any tenant. Therefore,
 * this repository has no tenant-filtered queries.</p>
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Finds a customer by their email address.
     * Used by {@code CustomerSecurityAdapter} for authentication lookup.
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Checks whether a customer with the given email already exists.
     * More efficient than {@code findByEmail().isPresent()} as it avoids
     * loading the entire entity — uses a {@code SELECT COUNT} or {@code EXISTS} query.
     */
    boolean existsByEmail(String email);

    /** Paginated customer list, newest-first — used by SuperAdmin customer management table. */
    Page<Customer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Count of banned customers — used for SuperAdmin platform KPI stats. */
    long countByIsActiveFalse();
}