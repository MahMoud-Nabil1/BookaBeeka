package com.system.booking.modules.availability.internal.repository;

import com.system.booking.modules.availability.api.SlotDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SlotQueryRepository {
    /**
     * Executes native PostgreSQL query to generate available slots.
     */
    List<SlotDto> generateSlots(UUID tenantId, UUID resourceId, LocalDate date, int durationMinutes, int bufferMinutes);
}
