package com.system.booking.modules.availability.internal.service;
import com.system.booking.modules.availability.api.ExceptionDto;
import com.system.booking.modules.availability.internal.entity.AvailabilityException;
import com.system.booking.modules.availability.internal.repository.AvailabilityExceptionRepository;
import com.system.booking.modules.inventory.internal.entity.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityExceptionService {
    private final AvailabilityExceptionRepository exceptionRepository;

    @Transactional
    public void addAvailabilityException(UUID tenantId, Resource resource, ExceptionDto dto) {
        AvailabilityException exception = AvailabilityException.builder()
                .tenantId(tenantId).resource(resource)
                .exceptionDate(dto.exceptionDate()).isAvailable(dto.isAvailable())
                .startTime(dto.startTime()).endTime(dto.endTime()).reason(dto.reason())
                .build();
        exceptionRepository.save(exception);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityException> listExceptions(UUID resourceId) {
        return exceptionRepository.findByResourceId(resourceId);
    }

    @Transactional
    public void updateException(UUID tenantId, UUID exceptionId, ExceptionDto dto) {
        AvailabilityException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new EntityNotFoundException("Exception not found"));
        if (!exception.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Exception not found for tenant");
        }
        exception.setExceptionDate(dto.exceptionDate());
        exception.setIsAvailable(dto.isAvailable());
        exception.setStartTime(dto.startTime());
        exception.setEndTime(dto.endTime());
        exception.setReason(dto.reason());
        exceptionRepository.save(exception);
    }

    @Transactional
    public void deleteException(UUID tenantId, UUID exceptionId) {
        AvailabilityException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new EntityNotFoundException("Exception not found"));
        if (!exception.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Exception not found for tenant");
        }
        exceptionRepository.delete(exception);
    }
}
