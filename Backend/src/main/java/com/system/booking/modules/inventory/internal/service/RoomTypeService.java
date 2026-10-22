package com.system.booking.modules.inventory.internal.service;

import com.system.booking.common.config.CacheConfig;
import com.system.booking.modules.inventory.internal.dto.request.RoomTypeCreateRequest;
import com.system.booking.modules.inventory.internal.dto.request.RoomTypeUpdateRequest;
import com.system.booking.modules.inventory.internal.dto.response.RoomTypeResponse;
import com.system.booking.modules.inventory.internal.entity.RoomType;
import com.system.booking.modules.inventory.internal.exception.DuplicateInventoryEntityException;
import com.system.booking.modules.inventory.internal.exception.RoomTypeNotFoundException;
import com.system.booking.modules.inventory.internal.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
     * Evicts the tenant's room-type list cache so the next list call is fresh.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ROOM_TYPES, key = "#tenantId")
    })
    public RoomTypeResponse createRoomType(UUID tenantId, RoomTypeCreateRequest req) {
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
     * Cached individually by tenantId + roomTypeId.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ROOM_TYPES,
               key = "#tenantId + '-' + #roomTypeId",
               unless = "#result == null")
    public RoomTypeResponse getRoomType(UUID tenantId, UUID roomTypeId) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));
        return toResponse(roomType);
    }

    /**
     * Lists all room types belonging to the given tenant.
     * Cached by tenantId — evicted on any write to room types for that tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ROOM_TYPES, key = "#tenantId", unless = "#result == null")
    public List<RoomTypeResponse> listRoomTypes(UUID tenantId) {
        return roomTypeRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates a room type. Evicts both the list cache and the single-entry cache
     * for this tenant, so any cached read is refreshed.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ROOM_TYPES, key = "#tenantId"),
            @CacheEvict(value = CacheConfig.CACHE_ROOM_TYPES, key = "#tenantId + '-' + #roomTypeId")
    })
    public RoomTypeResponse updateRoomType(UUID tenantId, UUID roomTypeId, RoomTypeUpdateRequest req) {
        RoomType roomType = roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId));

        if (req.name() != null && !req.name().trim().equalsIgnoreCase(roomType.getName())) {
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
     * Deletes a room type. Evicts the list and single-entry caches for this tenant.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_ROOM_TYPES, key = "#tenantId"),
            @CacheEvict(value = CacheConfig.CACHE_ROOM_TYPES, key = "#tenantId + '-' + #roomTypeId")
    })
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