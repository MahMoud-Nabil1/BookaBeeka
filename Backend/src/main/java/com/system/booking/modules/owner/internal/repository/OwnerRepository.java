package com.system.booking.modules.owner.internal.repository;

import com.system.booking.modules.owner.internal.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {
    Optional<Owner> findByEmail(String email);
    Optional<Owner> findByTenantId(UUID tenantId);
    boolean existsByEmail(String email);

    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    @Query("SELECT o.tenantId, COUNT(o) FROM Owner o WHERE o.tenantId IN :tenantIds AND o.isActive = true GROUP BY o.tenantId")
    List<Object[]> countActiveByTenantIds(@Param("tenantIds") List<UUID> tenantIds);
}