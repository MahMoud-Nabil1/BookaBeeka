package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.request.RoomTypeCreateRequest;
import com.system.booking.modules.inventory.internal.dto.request.RoomTypeUpdateRequest;
import com.system.booking.modules.inventory.internal.dto.response.RoomTypeResponse;
import com.system.booking.modules.inventory.internal.entity.RoomType;
import com.system.booking.modules.inventory.internal.exception.DuplicateInventoryEntityException;
import com.system.booking.modules.inventory.internal.exception.RoomTypeNotFoundException;
import com.system.booking.modules.inventory.internal.repository.RoomTypeRepository;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public RoomTypeResponse createRoomType(UUID tenantId, RoomTypeCreateRequest req) {
        Branch branch = branchRepository.findById(req.branchId())
                .filter(b -> b.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Branch not found or does not belong to tenant"));

        if (roomTypeRepository.existsByTenantIdAndBranchIdAndNameIgnoreCase(tenantId, req.branchId(), req.name().trim())) {
            throw new DuplicateInventoryEntityException("Room type '" + req.name() + "' already exists in this branch");
        }

        RoomType roomType = RoomType.builder()
                .tenantId(tenantId)
                .branch(branch)
                .name(req.name().trim())
                .description(req.description())
                .capacity(req.capacity())
                .basePricePerNight(req.basePricePerNight())
                .isActive(true)
                .build();

        roomType = roomTypeRepository.save(roomType);
        return toResponse(roomType);
    }

    @Transactional(readOnly = true)
    public RoomTypeResponse getRoomType(UUID tenantId, UUID roomTypeId) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        return toResponse(roomType);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> listRoomTypes(UUID tenantId) {
        return roomTypeRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> listRoomTypesByBranch(UUID tenantId, UUID branchId) {
        return roomTypeRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomTypeResponse updateRoomType(UUID tenantId, UUID roomTypeId, RoomTypeUpdateRequest req) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));

        if (req.name() != null && !req.name().trim().equalsIgnoreCase(roomType.getName())) {
            if (roomTypeRepository.existsByTenantIdAndBranchIdAndNameIgnoreCaseAndIdNot(
                    tenantId, roomType.getBranch().getId(), req.name().trim(), roomTypeId)) {
                throw new DuplicateInventoryEntityException("Room type '" + req.name() + "' already exists in this branch");
            }
            roomType.setName(req.name().trim());
        }

        if (req.description() != null) roomType.setDescription(req.description());
        if (req.capacity() != null) roomType.setCapacity(req.capacity());
        if (req.basePricePerNight() != null) roomType.setBasePricePerNight(req.basePricePerNight());
        if (req.isActive() != null) roomType.setIsActive(req.isActive());

        roomType = roomTypeRepository.save(roomType);
        return toResponse(roomType);
    }

    @Transactional
    public void deleteRoomType(UUID tenantId, UUID roomTypeId) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        roomTypeRepository.delete(roomType);
    }

    private RoomTypeResponse toResponse(RoomType rt) {
        return new RoomTypeResponse(
                rt.getId(),
                rt.getTenantId(),
                rt.getBranch() != null ? rt.getBranch().getId() : null,
                rt.getName(),
                rt.getDescription(),
                rt.getCapacity(),
                rt.getBasePricePerNight(),
                rt.getIsActive(),
                rt.getCreatedAt() != null ? rt.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}