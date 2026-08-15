package com.system.booking.modules.tenant.internal.repository;

import com.system.booking.modules.tenant.internal.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    // هنجيب بيها كل الفروع اللي تبع عيادة معينة
    List<Branch> findAllByTenantId(UUID tenantId);
}