package com.system.booking.modules.inventory.internal.repository;

import com.system.booking.modules.inventory.internal.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findByTenantId(UUID tenantId);
    List<Resource> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
    List<Resource> findByTenantIdAndRoomTypeId(UUID tenantId, UUID roomTypeId);
    Optional<Resource> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndBranchIdAndRoomNumber(UUID tenantId, UUID branchId, String roomNumber);
    boolean existsByTenantIdAndBranchIdAndRoomNumberAndIdNot(UUID tenantId, UUID branchId, String roomNumber, UUID id);
    boolean existsByTenantIdAndId(UUID tenantId, UUID id);
}