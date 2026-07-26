package com.system.booking.modules.availability.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public API for the Availability Engine module.
 * Other modules should only interact with availability through this interface.
 */
public interface AvailabilityModuleApi {

    List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, LocalDate date);

    SlotLockDto lockSlot(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end, UUID userId);

    void releaseLock(UUID tenantId, UUID lockId);

    void consumeLock(UUID tenantId, UUID lockId, UUID bookingId);

    boolean isRangeAvailable(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end);

    void defineScheduleRule(UUID tenantId, UUID resourceId, ScheduleRuleDto rule);

    void addAvailabilityException(UUID tenantId, UUID resourceId, ExceptionDto exception);
}
