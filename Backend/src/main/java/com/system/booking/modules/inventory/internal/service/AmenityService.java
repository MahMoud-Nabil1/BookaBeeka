package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.request.CreateAmenityRequest;
import com.system.booking.modules.inventory.internal.dto.request.UpdateAmenityRequest;
import com.system.booking.modules.inventory.internal.dto.response.AmenityResponse;
import com.system.booking.modules.inventory.internal.entity.Amenity;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ResourceAmenity;
import com.system.booking.modules.inventory.internal.exception.AmenityNotFoundException;
import com.system.booking.modules.inventory.internal.exception.CrossTenantViolationException;
import com.system.booking.modules.inventory.internal.exception.DuplicateInventoryEntityException;
import com.system.booking.modules.inventory.internal.exception.ResourceNotFoundException;
import com.system.booking.modules.inventory.internal.repository.AmenityRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceAmenityRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final ResourceAmenityRepository resourceAmenityRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public AmenityResponse createAmenity(UUID tenantId, CreateAmenityRequest req) {
        if (amenityRepository.existsByTenantIdAndNameIgnoreCase(tenantId, req.name().trim())) {
            throw new DuplicateInventoryEntityException("Amenity '" + req.name() + "' already exists for this tenant");
        }

        Amenity amenity = Amenity.builder()
                .tenantId(tenantId)
                .name(req.name().trim())
                .description(req.description())
                .iconUrl(req.iconUrl())
                .isActive(true)
                .build();

        amenity = amenityRepository.save(amenity);
        return toResponse(amenity);
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> listAmenities(UUID tenantId) {
        return amenityRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AmenityResponse getAmenity(UUID tenantId, UUID amenityId) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        return toResponse(amenity);
    }

    @Transactional
    public AmenityResponse updateAmenity(UUID tenantId, UUID amenityId, UpdateAmenityRequest req) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));

        if (req.name() != null && !req.name().trim().equalsIgnoreCase(amenity.getName())) {
            if (amenityRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, req.name().trim(), amenityId)) {
                throw new DuplicateInventoryEntityException("Amenity '" + req.name() + "' already exists for this tenant");
            }
            amenity.setName(req.name().trim());
        }

        if (req.description() != null) amenity.setDescription(req.description());
        if (req.iconUrl() != null) amenity.setIconUrl(req.iconUrl());
        if (req.isActive() != null) amenity.setIsActive(req.isActive());

        amenity = amenityRepository.save(amenity);
        return toResponse(amenity);
    }

    @Transactional
    public void deleteAmenity(UUID tenantId, UUID amenityId) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        amenityRepository.delete(amenity);
    }

    @Transactional
    public void linkAmenityToResource(UUID tenantId, UUID resourceId, UUID amenityId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));

        if (!resource.getTenantId().equals(tenantId) || !amenity.getTenantId().equals(tenantId)) {
            throw new CrossTenantViolationException("Resource and Amenity must belong to the authenticated tenant");
        }

        resourceAmenityRepository.findByTenantIdAndResourceIdAndAmenityId(tenantId, resourceId, amenityId)
                .orElseGet(() -> {
                    ResourceAmenity link = ResourceAmenity.builder()
                            .tenantId(tenantId)
                            .resource(resource)
                            .amenity(amenity)
                            .build();
                    return resourceAmenityRepository.save(link);
                });
    }

    @Transactional
    public void unlinkAmenityFromResource(UUID tenantId, UUID resourceId, UUID amenityId) {
        resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        resourceAmenityRepository.deleteByTenantIdAndResourceIdAndAmenityId(tenantId, resourceId, amenityId);
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> listAmenitiesForResource(UUID tenantId, UUID resourceId) {
        if (!resourceRepository.existsByTenantIdAndId(tenantId, resourceId)) {
            throw new ResourceNotFoundException(resourceId);
        }
        return resourceAmenityRepository.findByTenantIdAndResourceId(tenantId, resourceId).stream()
                .map(link -> toResponse(link.getAmenity()))
                .collect(Collectors.toList());
    }

    private AmenityResponse toResponse(Amenity a) {
        return new AmenityResponse(
                a.getId(),
                a.getTenantId(),
                a.getName(),
                a.getDescription(),
                a.getIconUrl(),
                a.getIsActive(),
                a.getCreatedAt() != null ? a.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}