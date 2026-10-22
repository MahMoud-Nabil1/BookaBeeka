package com.system.booking.modules.inventory.internal.repository;

import com.system.booking.modules.inventory.internal.entity.ResourceAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceAmenityRepository extends JpaRepository<ResourceAmenity, UUID> {

    List<ResourceAmenity> findByResourceId(UUID resourceId);
    List<ResourceAmenity> findByTenantIdAndResourceId(UUID tenantId, UUID resourceId);
    Optional<ResourceAmenity> findByResourceIdAndAmenityId(UUID resourceId, UUID amenityId);
    Optional<ResourceAmenity> findByTenantIdAndResourceIdAndAmenityId(UUID tenantId, UUID resourceId, UUID amenityId);
    boolean existsByTenantIdAndResourceIdAndAmenityId(UUID tenantId, UUID resourceId, UUID amenityId);
    void deleteByResourceIdAndAmenityId(UUID resourceId, UUID amenityId);
    void deleteByTenantIdAndResourceIdAndAmenityId(UUID tenantId, UUID resourceId, UUID amenityId);

    @Query("SELECT ra.resource.id, a.name FROM ResourceAmenity ra JOIN ra.amenity a WHERE ra.resource.id IN :resourceIds")
    List<Object[]> findAmenityNamesByResourceIds(@Param("resourceIds") List<UUID> resourceIds);
}
