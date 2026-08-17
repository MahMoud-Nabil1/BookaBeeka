package com.system.booking.modules.admin.internal.repository;

import com.system.booking.modules.admin.internal.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, UUID> {

    /** Used by the login flow to look up an admin by email. */
    Optional<SuperAdmin> findByEmail(String email);
}
