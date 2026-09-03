package com.system.booking.modules.inventory.internal.repository;

import com.system.booking.modules.inventory.internal.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    List<Amenity> findByTenantId(UUID tenantId);
    Optional<Amenity> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Amenity> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
