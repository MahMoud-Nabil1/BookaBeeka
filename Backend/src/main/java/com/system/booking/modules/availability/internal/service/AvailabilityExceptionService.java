package com.system.booking.modules.availability.internal.service;

import com.system.booking.modules.availability.api.ExceptionDto;
import com.system.booking.modules.availability.internal.entity.AvailabilityException;
import com.system.booking.modules.availability.internal.repository.AvailabilityExceptionRepository;
import com.system.booking.modules.inventory.internal.entity.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityExceptionService {

    private final AvailabilityExceptionRepository exceptionRepository;

    @Transactional
    public void addAvailabilityException(UUID tenantId, Resource resource, ExceptionDto dto) {
        AvailabilityException exception = AvailabilityException.builder()
                .tenantId(tenantId)
                .resource(resource)
                .exceptionDate(dto.exceptionDate())
                .isAvailable(dto.isAvailable())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .reason(dto.reason())
                .build();
                
        exceptionRepository.save(exception);
    }
}
