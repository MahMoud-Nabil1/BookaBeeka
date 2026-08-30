package com.system.booking.modules.inventory.internal.repository;

import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    List<ServiceOffering> findByTenantId(UUID tenantId);
    Optional<ServiceOffering> findByTenantIdAndId(UUID tenantId, UUID id);
}
