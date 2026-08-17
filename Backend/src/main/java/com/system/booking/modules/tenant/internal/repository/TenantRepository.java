package com.system.booking.modules.tenant.internal.repository;

import com.system.booking.modules.tenant.internal.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /** Paginated list of all tenants, newest-first — used by SuperAdmin tenant table. */
    Page<Tenant> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Counts tenants in a given status — used for SuperAdmin platform KPI stats. */
    long countByStatus(String status);

    /** Lookup by subdomain — used during tenant onboarding checks. */
    Optional<Tenant> findBySubdomain(String subdomain);
}
