package com.system.booking.modules.inventory.internal.repository;

import com.system.booking.modules.inventory.internal.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByTenantId(UUID tenantId);
    List<RoomType> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
    Optional<RoomType> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndBranchIdAndNameIgnoreCase(UUID tenantId, UUID branchId, String name);
    boolean existsByTenantIdAndBranchIdAndNameIgnoreCaseAndIdNot(UUID tenantId, UUID branchId, String name, UUID id);
}