package com.system.booking.modules.availability.api;
import com.system.booking.modules.availability.internal.service.AvailabilityExceptionService;
import com.system.booking.modules.availability.internal.service.ScheduleRuleService;
import com.system.booking.modules.availability.internal.service.SlotGenerationService;
import com.system.booking.modules.availability.internal.service.SlotLockingService;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.dto.ServiceOfferingResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityModuleApiImpl implements AvailabilityModuleApi {
    private final ScheduleRuleService scheduleRuleService;
    private final AvailabilityExceptionService exceptionService;
    private final SlotGenerationService slotGenerationService;
    private final SlotLockingService slotLockingService;
    private final EntityManager entityManager;
    private final InventoryModuleApi inventoryApi;

    @Override
    public List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, LocalDate date) {
        return slotGenerationService.generateSlots(tenantId, resourceId, date, 60, 0);
    }
    
    @Override
    public List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, UUID serviceOfferingId, LocalDate date) {
        ServiceOfferingResponse service = inventoryApi.getServiceOfferingByTenantAndId(tenantId, serviceOfferingId);
        int duration = service.durationMinutes();
        int buffer = service.bufferMinutes() != null ? service.bufferMinutes() : 0;
        return slotGenerationService.generateSlots(tenantId, resourceId, date, duration, buffer);
    }
    
    @Override
    public SlotLockDto lockSlot(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end, UUID userId) {
        Resource resource = entityManager.getReference(Resource.class, resourceId);
        return slotLockingService.acquireTemporaryLock(tenantId, resource, start, end, userId);
    }
    @Override
    public void releaseLock(UUID tenantId, UUID lockId) { slotLockingService.releaseLock(tenantId, lockId); }
    @Override
    public void consumeLock(UUID tenantId, UUID lockId, UUID bookingId) { slotLockingService.consumeLock(tenantId, lockId, bookingId); }
    @Override
    public boolean isRangeAvailable(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end) {
        LocalDate date = start.toLocalDate();
        List<SlotDto> openSlots = slotGenerationService.generateSlots(tenantId, resourceId, date, 60, 0);
        return openSlots.stream().anyMatch(slot -> !slot.start().isAfter(start) && !slot.end().isBefore(end));
    }
    @Override
    public void defineScheduleRule(UUID tenantId, UUID resourceId, ScheduleRuleDto rule) {
        Resource resource = entityManager.getReference(Resource.class, resourceId);
        scheduleRuleService.defineScheduleRule(tenantId, resource, rule);
    }
    
    @Override
    public void updateScheduleRule(UUID tenantId, UUID ruleId, ScheduleRuleDto rule) {
        scheduleRuleService.updateScheduleRule(tenantId, ruleId, rule);
    }

    @Override
    public void deleteScheduleRule(UUID tenantId, UUID ruleId) {
        scheduleRuleService.deleteScheduleRule(tenantId, ruleId);
    }
    
    @Override
    public void addAvailabilityException(UUID tenantId, UUID resourceId, ExceptionDto exception) {
        Resource resource = entityManager.getReference(Resource.class, resourceId);
        exceptionService.addAvailabilityException(tenantId, resource, exception);
    }
    
    @Override
    public void updateAvailabilityException(UUID tenantId, UUID exceptionId, ExceptionDto exception) {
        exceptionService.updateException(tenantId, exceptionId, exception);
    }

    @Override
    public void deleteAvailabilityException(UUID tenantId, UUID exceptionId) {
        exceptionService.deleteException(tenantId, exceptionId);
    }
}
