package com.system.booking.modules.customer.internal.repository;

import com.system.booking.modules.customer.internal.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    /** Paginated customer list, newest-first — used by SuperAdmin customer management table. */
    Page<Customer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Count of banned customers — used for SuperAdmin platform KPI stats. */
    long countByIsActiveFalse();
}