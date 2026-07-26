package com.system.booking.modules.availability.internal.service;

import com.system.booking.modules.availability.api.SlotDto;
import com.system.booking.modules.availability.internal.repository.SlotQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotGenerationService {

    private final SlotQueryRepository slotQueryRepository;

    @Transactional(readOnly = true)
    public List<SlotDto> generateSlots(UUID tenantId, UUID resourceId, LocalDate date, int durationMinutes, int bufferMinutes) {
        // Runs the native PostgreSQL query to subtract exceptions and bookings from schedule rules
        return slotQueryRepository.generateSlots(tenantId, resourceId, date, durationMinutes, bufferMinutes);
    }
}
