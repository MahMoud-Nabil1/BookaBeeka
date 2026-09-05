package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.request.CreateResourceRequest;
import com.system.booking.modules.inventory.internal.dto.request.UpdateResourceRequest;
import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.RoomStatus;
import com.system.booking.modules.inventory.internal.entity.RoomType;
import com.system.booking.modules.inventory.internal.exception.DuplicateInventoryEntityException;
import com.system.booking.modules.inventory.internal.exception.ResourceNotFoundException;
import com.system.booking.modules.inventory.internal.exception.RoomTypeNotFoundException;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
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
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final BranchRepository branchRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Transactional
    public ResourceResponse createResource(UUID tenantId, CreateResourceRequest req) {
        Branch branch = branchRepository.findById(req.branchId())
                .filter(b -> b.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Branch not found or does not belong to tenant"));

        RoomType roomType = null;
        if (req.roomTypeId() != null) {
            roomType = roomTypeRepository.findByTenantIdAndId(tenantId, req.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(req.roomTypeId()));
            if (!roomType.getBranch().getId().equals(req.branchId())) {
                throw new IllegalArgumentException("Room type does not belong to the same branch");
            }
        }

        if (req.roomNumber() != null && !req.roomNumber().isBlank()) {
            if (resourceRepository.existsByTenantIdAndBranchIdAndRoomNumber(tenantId, req.branchId(), req.roomNumber().trim())) {
                throw new DuplicateInventoryEntityException("Room number '" + req.roomNumber() + "' already exists in this branch");
            }
        }

        Integer effectiveCapacity = req.capacity() != null ? req.capacity()
                : (roomType != null ? roomType.getCapacity() : null);

        Resource resource = Resource.builder()
                .tenantId(tenantId)
                .branch(branch)
                .roomType(roomType)
                .name(req.name().trim())
                .roomNumber(req.roomNumber() != null ? req.roomNumber().trim() : null)
                .floor(req.floor())
                .status(req.status() != null ? req.status() : RoomStatus.AVAILABLE)
                .resourceType(req.resourceType() != null ? req.resourceType() : "ROOM")
                .capacity(effectiveCapacity)
                .specs(req.specs())
                .isActive(true)
                .isBookable(true)
                .pricePerNight(req.pricePerNight() != null ? req.pricePerNight() : (roomType != null ? roomType.getBasePricePerNight() : null))
                .currency(req.currency() != null ? req.currency() : "USD")
                .build();

        resource = resourceRepository.save(resource);
        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse updateResource(UUID tenantId, UUID resourceId, UpdateResourceRequest req) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        if (req.roomTypeId() != null) {
            RoomType newType = roomTypeRepository.findByTenantIdAndId(tenantId, req.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(req.roomTypeId()));
            if (!newType.getBranch().getId().equals(resource.getBranch().getId())) {
                throw new IllegalArgumentException("New room type does not belong to the resource branch");
            }
            resource.setRoomType(newType);
        }

        if (req.roomNumber() != null && !req.roomNumber().isBlank()
                && !req.roomNumber().trim().equalsIgnoreCase(resource.getRoomNumber())) {
            if (resourceRepository.existsByTenantIdAndBranchIdAndRoomNumberAndIdNot(
                    tenantId, resource.getBranch().getId(), req.roomNumber().trim(), resourceId)) {
                throw new DuplicateInventoryEntityException("Room number '" + req.roomNumber() + "' already exists in this branch");
            }
            resource.setRoomNumber(req.roomNumber().trim());
        }

        if (req.name() != null) resource.setName(req.name().trim());
        if (req.floor() != null) resource.setFloor(req.floor());
        if (req.status() != null) resource.setStatus(req.status());
        if (req.resourceType() != null) resource.setResourceType(req.resourceType());
        if (req.capacity() != null) resource.setCapacity(req.capacity());
        if (req.specs() != null) resource.setSpecs(req.specs());
        if (req.isActive() != null) resource.setIsActive(req.isActive());
        if (req.isBookable() != null) resource.setIsBookable(req.isBookable());
        if (req.pricePerNight() != null) resource.setPricePerNight(req.pricePerNight());
        if (req.currency() != null) resource.setCurrency(req.currency());

        resource = resourceRepository.save(resource);
        return toResponse(resource);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID tenantId) {
        return resourceRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listResourcesByBranch(UUID tenantId, UUID branchId) {
        return resourceRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        return toResponse(resource);
    }

    @Transactional
    public void deleteResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        resourceRepository.delete(resource);
    }

    private ResourceResponse toResponse(Resource r) {
        return new ResourceResponse(
                r.getId(),
                r.getTenantId(),
                r.getBranch() != null ? r.getBranch().getId() : null,
                r.getRoomType() != null ? r.getRoomType().getId() : null,
                r.getRoomType() != null ? r.getRoomType().getName() : null,
                r.getName(),
                r.getRoomNumber(),
                r.getFloor(),
                r.getStatus(),
                r.getResourceType(),
                r.getCapacity(),
                r.getSpecs(),
                r.getIsActive(),
                r.getIsBookable(),
                r.getPricePerNight(),
                r.getCurrency(),
                r.getCreatedAt() != null ? r.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}