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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for CRUD operations on {@link Resource} entities (physical bookable units,
 * e.g., individual hotel rooms).
 *
 * <p><b>Multi-Tenancy Isolation Pattern:</b><br>
 * All read and write operations are scoped to a {@code tenantId} extracted from the caller's JWT.
 * No method accepts a client-supplied tenant identifier.</p>
 *
 * <p><b>Intra-Tenant RoomType Validation:</b><br>
 * When a {@code roomTypeId} is provided in the create or update request, the service resolves the
 * {@link RoomType} using {@code findByTenantIdAndId}. This is a critical cross-field isolation check:
 * it prevents an authenticated Owner of Tenant A from assigning a RoomType that belongs to Tenant B
 * to one of their own Resources. If the RoomType doesn't belong to the caller's tenant, the query
 * returns empty and a {@link RoomTypeNotFoundException} is thrown (404), intentionally not revealing
 * that the RoomType exists under a different tenant.</p>
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final RoomTypeRepository roomTypeRepository;

    /**
     * Creates a new resource (room) scoped to the given tenant.
     *
     * <p>If a {@code roomTypeId} is supplied, it is validated against the caller's tenant to prevent
     * cross-tenant RoomType assignment. Capacity and price-per-night fall back to the RoomType's
     * configured values if not explicitly provided in the request.</p>
     */
    @Transactional
    public ResourceResponse createResource(UUID tenantId, CreateResourceRequest req) {
        RoomType roomType = null;
        if (req.roomTypeId() != null) {
            // Validate that the specified RoomType belongs to the same tenant.
            // Using findByTenantIdAndId ensures cross-tenant RoomType assignment is silently rejected
            // with a 404 rather than a security error, to avoid exposing cross-tenant data existence.
            roomType = roomTypeRepository.findByTenantIdAndId(tenantId, req.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(req.roomTypeId()));
        }

        if (req.roomNumber() != null && !req.roomNumber().isBlank()) {
            // Guard against duplicate room numbers within the same tenant.
            if (resourceRepository.existsByTenantIdAndRoomNumber(tenantId, req.roomNumber().trim())) {
                throw new DuplicateInventoryEntityException("Room number '" + req.roomNumber() + "' already exists for this tenant");
            }
        }

        // Inherit capacity and price from RoomType when not explicitly overridden in the request.
        Integer effectiveCapacity = req.capacity() != null ? req.capacity()
                : (roomType != null ? roomType.getCapacity() : null);

        Resource resource = Resource.builder()
                .tenantId(tenantId)
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

    /**
     * Updates an existing resource, scoped to the given tenant.
     *
     * <p>If the request includes a new {@code roomTypeId}, the new RoomType is validated
     * against the caller's tenant before assignment — same cross-tenant protection as in
     * {@link #createResource}.</p>
     */
    @Transactional
    public ResourceResponse updateResource(UUID tenantId, UUID resourceId, UpdateResourceRequest req) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        if (req.roomTypeId() != null) {
            // Re-validate RoomType tenant membership on update, same as on create.
            RoomType newType = roomTypeRepository.findByTenantIdAndId(tenantId, req.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(req.roomTypeId()));
            resource.setRoomType(newType);
        }

        if (req.roomNumber() != null && !req.roomNumber().isBlank()
                && !req.roomNumber().trim().equalsIgnoreCase(resource.getRoomNumber())) {
            if (resourceRepository.existsByTenantIdAndRoomNumberAndIdNot(tenantId, req.roomNumber().trim(), resourceId)) {
                throw new DuplicateInventoryEntityException("Room number '" + req.roomNumber() + "' already exists for this tenant");
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

    /**
     * Lists all resources belonging to the given tenant.
     *
     * <p>Query is scoped to {@code tenantId} — only this tenant's rooms are returned.</p>
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID tenantId) {
        return resourceRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single resource by ID, scoped to the given tenant.
     *
     * <p>The compound {@code findByTenantIdAndId} lookup ensures a 404 is returned
     * if the resource belongs to a different tenant — consistent with the principle
     * of not revealing cross-tenant data existence.</p>
     */
    @Transactional(readOnly = true)
    public ResourceResponse getResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        return toResponse(resource);
    }

    /**
     * Deletes a resource, scoped to the given tenant.
     */
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