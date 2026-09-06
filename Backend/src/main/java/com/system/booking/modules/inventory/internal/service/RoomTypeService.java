package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.request.RoomTypeCreateRequest;
import com.system.booking.modules.inventory.internal.dto.request.RoomTypeUpdateRequest;
import com.system.booking.modules.inventory.internal.dto.response.RoomTypeResponse;
import com.system.booking.modules.inventory.internal.entity.RoomType;
import com.system.booking.modules.inventory.internal.exception.DuplicateInventoryEntityException;
import com.system.booking.modules.inventory.internal.exception.RoomTypeNotFoundException;
import com.system.booking.modules.inventory.internal.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for CRUD operations on {@link RoomType} entities.
 *
 * <p><b>Multi-Tenancy Isolation Pattern:</b><br>
 * Every public method accepts a {@code tenantId} parameter that is supplied by the
 * {@link com.system.booking.modules.inventory.api.InventoryController} from the
 * authenticated user's JWT. This value is never sourced from the request body.
 * All repository queries include {@code tenantId} as a predicate, meaning a caller
 * can only ever read or modify room types that belong to their own tenant — even if
 * they somehow obtain another tenant's UUID, the query will return empty results.</p>
 */
@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    /**
     * Creates a new room type scoped to the given tenant.
     *
     * <p>The pre-creation duplicate check uses a case-insensitive name match scoped to
     * {@code tenantId}. This mirrors the DB-level {@code uq_room_type_tenant_name}
     * composite unique constraint and provides a clean application-level error message
     * instead of letting a raw {@code DataIntegrityViolationException} propagate.</p>
     */
    @Transactional
    public RoomTypeResponse createRoomType(UUID tenantId, RoomTypeCreateRequest req) {
        // Guard against duplicate names within this tenant before hitting the DB constraint.
        // The check is case-insensitive to match the unique constraint semantics.
        if (roomTypeRepository.existsByTenantIdAndNameIgnoreCase(tenantId, req.name().trim())) {
            throw new DuplicateInventoryEntityException("Room type '" + req.name() + "' already exists for this tenant");
        }

        RoomType roomType = RoomType.builder()
                .tenantId(tenantId)
                .name(req.name().trim())
                .description(req.description())
                .capacity(req.capacity())
                .basePricePerNight(req.basePricePerNight())
                .isActive(true)
                .build();

        roomType = roomTypeRepository.save(roomType);
        return toResponse(roomType);
    }

    /**
     * Retrieves a single room type, scoped to the given tenant.
     *
     * <p>Uses {@code findByTenantIdAndId} — the compound key lookup ensures that a
     * tenant cannot access room types from a different tenant even with a valid UUID,
     * as the query will simply return empty and throw a {@link RoomTypeNotFoundException}
     * (404) rather than revealing that the resource exists under another tenant.</p>
     */
    @Transactional(readOnly = true)
    public RoomTypeResponse getRoomType(UUID tenantId, UUID roomTypeId) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        return toResponse(roomType);
    }

    /**
     * Lists all room types belonging to the given tenant.
     *
     * <p>Scoped to {@code tenantId} — Tenant B will never see Tenant A's room types
     * in this list, even if they are on the same database instance.</p>
     */
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> listRoomTypes(UUID tenantId) {
        return roomTypeRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates a room type, scoped to the given tenant.
     *
     * <p>Both the existence check ({@code findByTenantIdAndId}) and the rename duplicate
     * check ({@code existsByTenantIdAndNameIgnoreCaseAndIdNot}) are scoped to the tenant,
     * providing full isolation during update operations.</p>
     */
    @Transactional
    public RoomTypeResponse updateRoomType(UUID tenantId, UUID roomTypeId, RoomTypeUpdateRequest req) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));

        if (req.name() != null && !req.name().trim().equalsIgnoreCase(roomType.getName())) {
            // Check for name collision only if the name is actually changing.
            if (roomTypeRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, req.name().trim(), roomTypeId)) {
                throw new DuplicateInventoryEntityException("Room type '" + req.name() + "' already exists for this tenant");
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

    /**
     * Deletes a room type, scoped to the given tenant.
     *
     * <p>The tenant-scoped lookup ensures a tenant can only delete their own room types.</p>
     */
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
                rt.getName(),
                rt.getDescription(),
                rt.getCapacity(),
                rt.getBasePricePerNight(),
                rt.getIsActive(),
                rt.getCreatedAt() != null ? rt.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}