package com.system.booking.modules.availability.internal.service;

import com.system.booking.modules.availability.api.SlotLockDto;
import com.system.booking.modules.availability.internal.entity.SlotLock;
import com.system.booking.modules.availability.internal.repository.SlotLockRepository;
import com.system.booking.modules.inventory.internal.entity.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotLockingService {

    private final SlotLockRepository slotLockRepository;

    @Transactional
    public SlotLockDto acquireTemporaryLock(UUID tenantId, Resource resource, OffsetDateTime start, OffsetDateTime end, UUID userId) {
        // In a full implementation, we'd add an exclusion constraint check here or SELECT FOR UPDATE
        
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(10);
        
        SlotLock lock = SlotLock.builder()
                .tenantId(tenantId)
                .resource(resource)
                .slotStart(start)
                .slotEnd(end)
                .userId(userId)
                .status("ACTIVE")
                .expiresAt(expiresAt)
                .build();
                
        lock = slotLockRepository.save(lock);
        
        return new SlotLockDto(lock.getId(), lock.getSlotStart(), lock.getSlotEnd(), lock.getExpiresAt(), lock.getStatus());
    }

    @Transactional
    public void releaseLock(UUID tenantId, UUID lockId) {
        slotLockRepository.findById(lockId).ifPresent(lock -> {
            if (lock.getTenantId().equals(tenantId) && "ACTIVE".equals(lock.getStatus())) {
                lock.setStatus("RELEASED");
                slotLockRepository.save(lock);
            }
        });
    }

    @Transactional
    public void consumeLock(UUID tenantId, UUID lockId, UUID bookingId) {
        slotLockRepository.findById(lockId).ifPresent(lock -> {
            if (lock.getTenantId().equals(tenantId) && "ACTIVE".equals(lock.getStatus())) {
                lock.setStatus("CONSUMED");
                slotLockRepository.save(lock);
            }
        });
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireStaleLocks() {
        int expired = slotLockRepository.expireStaleLocks();
        if (expired > 0) {
            log.info("Expired {} stale slot locks", expired);
        }
    }
}
