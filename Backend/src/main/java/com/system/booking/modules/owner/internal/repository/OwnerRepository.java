package com.system.booking.modules.owner.internal.repository;

import com.system.booking.modules.owner.internal.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {
    Optional<Owner> findByEmail(String email);
    Optional<Owner> findByTenantId(UUID tenantId);
    boolean existsByEmail(String email);
}