package com.system.booking.modules.hoteladmin.internal.repository;

import com.system.booking.modules.hoteladmin.internal.entity.HotelAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HotelAdminRepository extends JpaRepository<HotelAdmin, UUID> {

    Optional<HotelAdmin> findByEmail(String email);

    Optional<HotelAdmin> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByEmail(String email);

    List<HotelAdmin> findByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);
}