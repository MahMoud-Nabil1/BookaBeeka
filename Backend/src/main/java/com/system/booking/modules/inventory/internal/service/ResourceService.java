package com.system.booking.modules.inventory.internal.service;

import com.system.booking.common.config.CacheConfig;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for CRUD operations on {@link Resource} (room) entities.
 *
 * <p><b>Multi-Tenancy Isolation Pattern:</b><br>
 * Every public method accepts a {@code tenantId} extracted from the JWT.
 * All repository queries include tenantId as a predicate.</p>
 *
 * <p><b>Intra-Tenant RoomType Validation:</b><br>
 * When a {@code roomTypeId} is provided, the service resolves the {@link RoomType}
 * using {@code findByTenantIdAndId} to prevent cross-tenant RoomType assignment.</p>
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final RoomTypeRepository roomTypeRepository;

    /**
     * Creates a new resource (room). Evicts the tenant's room list cache.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_RESOURCES, key = "#tenantId")
    public ResourceResponse createResource(UUID tenantId, CreateResourceRequest req) {
        RoomType roomType = null;
        if (req.roomTypeId() != null) {
            roomType = roomTypeRepository.findByTenantIdAndId(tenantId, req.roomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException(req.roomTypeId()));
        }

        if (req.roomNumber() != null && !req.roomNumber().isBlank()) {
            if (resourceRepository.existsByTenantIdAndRoomNumber(tenantId, req.roomNumber().trim())) {
                throw new DuplicateInventoryEntityException("Room number '" + req.roomNumber() + "' already exists for this tenant");
            }
        }

        Integer effectiveCapacity = req.capacity() != null ? req.capacity()
                : (roomType != null ? roomType.getCapacity() : null);

        BigDecimal effectivePrice = req.pricePerNight();
        if (effectivePrice == null && req.specs() != null && req.specs().get("pricePerNight") != null) {
            effectivePrice = parseBigDecimal(req.specs().get("pricePerNight"));
        }
        if (effectivePrice == null && roomType != null) {
            effectivePrice = roomType.getBasePricePerNight();
        }

        Map<String, Object> effectiveSpecs = req.specs() != null ? new HashMap<>(req.specs()) : new HashMap<>();
        if (effectivePrice != null && !effectiveSpecs.containsKey("pricePerNight")) {
            effectiveSpecs.put("pricePerNight", effectivePrice);
        }

        Resource resource = Resource.builder()
                .tenantId(tenantId)
                .roomType(roomType)
                .name(req.name().trim())
                .roomNumber(req.roomNumber() != null ? req.roomNumber().trim() : null)
                .floor(req.floor())
                .status(req.status() != null ? req.status() : RoomStatus.AVAILABLE)
                .resourceType(req.resourceType() != null ? req.resourceType() : "ROOM")
                .capacity(effectiveCapacity)
                .specs(effectiveSpecs)
                .isActive(true)
                .isBookable(true)
                .pricePerNight(effectivePrice)
                .currency(req.currency() != null ? req.currency() : "USD")
                .build();

        resource = resourceRepository.save(resource);
        return toResponse(resource);
    }

    /**
     * Updates a resource. Evicts both the list cache and the single-resource cache.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_RESOURCES, key = "#tenantId"),
            @CacheEvict(value = CacheConfig.CACHE_RESOURCE, key = "#tenantId + '-' + #resourceId")
    })
    public ResourceResponse updateResource(UUID tenantId, UUID resourceId, UpdateResourceRequest req) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        if (req.roomTypeId() != null) {
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

        BigDecimal newPrice = req.pricePerNight();
        if (newPrice == null && req.specs() != null && req.specs().get("pricePerNight") != null) {
            newPrice = parseBigDecimal(req.specs().get("pricePerNight"));
        }
        if (newPrice != null) {
            resource.setPricePerNight(newPrice);
        }

        if (req.name() != null) resource.setName(req.name().trim());
        if (req.floor() != null) resource.setFloor(req.floor());
        if (req.status() != null) resource.setStatus(req.status());
        if (req.resourceType() != null) resource.setResourceType(req.resourceType());
        if (req.capacity() != null) resource.setCapacity(req.capacity());
        if (req.specs() != null) {
            Map<String, Object> specsMap = new HashMap<>(req.specs());
            if (resource.getPricePerNight() != null && !specsMap.containsKey("pricePerNight")) {
                specsMap.put("pricePerNight", resource.getPricePerNight());
            }
            resource.setSpecs(specsMap);
        } else if (resource.getPricePerNight() != null && (resource.getSpecs() == null || !resource.getSpecs().containsKey("pricePerNight"))) {
            Map<String, Object> specsMap = resource.getSpecs() != null ? new HashMap<>(resource.getSpecs()) : new HashMap<>();
            specsMap.put("pricePerNight", resource.getPricePerNight());
            resource.setSpecs(specsMap);
        }
        if (req.isActive() != null) resource.setIsActive(req.isActive());
        if (req.isBookable() != null) resource.setIsBookable(req.isBookable());
        if (req.currency() != null) resource.setCurrency(req.currency());

        resource = resourceRepository.save(resource);
        return toResponse(resource);
    }

    /**
     * Lists all resources for a tenant.
     * Cached by tenantId — evicted on any write.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_RESOURCES, key = "#tenantId", unless = "#result == null")
    public List<ResourceResponse> listResources(UUID tenantId) {
        return resourceRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single resource by ID.
     * Cached individually by tenantId + resourceId.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_RESOURCE,
               key = "#tenantId + '-' + #resourceId",
               unless = "#result == null")
    public ResourceResponse getResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        return toResponse(resource);
    }

    /**
     * Deletes a resource. Evicts both the list and single-resource caches.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_RESOURCES, key = "#tenantId"),
            @CacheEvict(value = CacheConfig.CACHE_RESOURCE, key = "#tenantId + '-' + #resourceId")
    })
    public void deleteResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        resourceRepository.delete(resource);
    }

    private ResourceResponse toResponse(Resource r) {
        BigDecimal effectivePrice = r.getPricePerNight();
        if (effectivePrice == null && r.getSpecs() != null && r.getSpecs().get("pricePerNight") != null) {
            effectivePrice = parseBigDecimal(r.getSpecs().get("pricePerNight"));
        }
        if (effectivePrice == null && r.getRoomType() != null) {
            effectivePrice = r.getRoomType().getBasePricePerNight();
        }

        Map<String, Object> responseSpecs = r.getSpecs() != null ? new HashMap<>(r.getSpecs()) : new HashMap<>();
        if (effectivePrice != null && !responseSpecs.containsKey("pricePerNight")) {
            responseSpecs.put("pricePerNight", effectivePrice);
        }

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
                responseSpecs,
                r.getIsActive(),
                r.getIsBookable(),
                effectivePrice,
                r.getCurrency(),
                r.getCreatedAt() != null ? r.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}