package com.system.booking.modules.booking.internal.repository;

import com.system.booking.modules.booking.internal.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, UUID> {

    // sorted descending so we can walk from the widest window down
    List<CancellationPolicy> findByTenantIdOrderByHoursBeforeSlotDesc(UUID tenantId);
}
