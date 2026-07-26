package com.system.booking.modules.availability.internal.repository;

import com.system.booking.modules.availability.internal.entity.SlotLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SlotLockRepository extends JpaRepository<SlotLock, UUID> {
    
    @Query("SELECT sl FROM SlotLock sl WHERE sl.resource.id = :resourceId AND sl.status = 'ACTIVE' AND sl.expiresAt > CURRENT_TIMESTAMP")
    List<SlotLock> findActiveLocksForResource(@Param("resourceId") UUID resourceId);

    @Modifying
    @Query("UPDATE SlotLock sl SET sl.status = 'EXPIRED' WHERE sl.status = 'ACTIVE' AND sl.expiresAt < CURRENT_TIMESTAMP")
    int expireStaleLocks();
}
