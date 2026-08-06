package com.system.booking.modules.booking.internal.repository;

import com.system.booking.modules.booking.internal.entity.IdempotencyKey;
import com.system.booking.modules.booking.internal.entity.IdempotencyKeyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {
}
