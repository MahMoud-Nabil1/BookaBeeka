package com.system.booking.modules.availability.internal.service;
import com.system.booking.modules.availability.api.ExceptionDto;
import com.system.booking.modules.availability.api.dto.CreateRoomBlockRequest;
import com.system.booking.modules.availability.api.dto.UpdateRoomBlockRequest;
import com.system.booking.modules.availability.api.dto.RoomBlockResponse;
import com.system.booking.modules.availability.internal.entity.AvailabilityException;
import com.system.booking.modules.availability.internal.repository.AvailabilityExceptionRepository;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityExceptionService {
    private final AvailabilityExceptionRepository exceptionRepository;
    private final ResourceRepository resourceRepository;

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

    // ── Hotel Room Block Operations (tenant-scoped via JWT) ──────────────

    @Transactional
    public RoomBlockResponse createRoomBlock(UUID tenantId, CreateRoomBlockRequest req) {
        if (!req.startDate().isBefore(req.endDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, req.resourceId())
                .orElseThrow(() -> new EntityNotFoundException("Resource not found for this tenant"));

        // Check for overlapping blocks
        List<AvailabilityException> overlapping = exceptionRepository.findOverlappingBlocks(
                req.resourceId(), req.startDate(), req.endDate());
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Overlapping room block already exists for this date range");
        }

        AvailabilityException block = AvailabilityException.builder()
                .tenantId(tenantId)
                .resource(resource)
                .isAvailable(false)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .reason(req.reason())
                .build();
        block = exceptionRepository.save(block);
        return toBlockResponse(block);
    }

    @Transactional(readOnly = true)
    public List<RoomBlockResponse> listRoomBlocks(UUID tenantId) {
        return exceptionRepository.findByTenantIdAndStartDateIsNotNull(tenantId).stream()
                .map(this::toBlockResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomBlockResponse getRoomBlock(UUID tenantId, UUID blockId) {
        AvailabilityException block = exceptionRepository.findByTenantIdAndId(tenantId, blockId)
                .orElseThrow(() -> new EntityNotFoundException("Room block not found"));
        if (block.getStartDate() == null) {
            throw new EntityNotFoundException("Room block not found");
        }
        return toBlockResponse(block);
    }

    @Transactional
    public RoomBlockResponse updateRoomBlock(UUID tenantId, UUID blockId, UpdateRoomBlockRequest req) {
        AvailabilityException block = exceptionRepository.findByTenantIdAndId(tenantId, blockId)
                .orElseThrow(() -> new EntityNotFoundException("Room block not found"));
        if (block.getStartDate() == null) {
            throw new EntityNotFoundException("Room block not found");
        }

        if (req.startDate() != null) block.setStartDate(req.startDate());
        if (req.endDate() != null) block.setEndDate(req.endDate());
        if (req.reason() != null) block.setReason(req.reason());

        if (!block.getStartDate().isBefore(block.getEndDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        block = exceptionRepository.save(block);
        return toBlockResponse(block);
    }

    @Transactional
    public void deleteRoomBlock(UUID tenantId, UUID blockId) {
        AvailabilityException block = exceptionRepository.findByTenantIdAndId(tenantId, blockId)
                .orElseThrow(() -> new EntityNotFoundException("Room block not found"));
        if (!block.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Room block not found for tenant");
        }
        exceptionRepository.delete(block);
    }

    private RoomBlockResponse toBlockResponse(AvailabilityException block) {
        String roomName = block.getResource() != null ? block.getResource().getName() : null;
        return new RoomBlockResponse(
                block.getId(), block.getResource().getId(), roomName,
                block.getStartDate(), block.getEndDate(),
                block.getReason(), block.getCreatedAt()
        );
    }
}
