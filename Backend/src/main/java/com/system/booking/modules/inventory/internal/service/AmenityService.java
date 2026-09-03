package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.AmenityResponse;
import com.system.booking.modules.inventory.internal.dto.CreateAmenityRequest;
import com.system.booking.modules.inventory.internal.dto.UpdateAmenityRequest;
import com.system.booking.modules.inventory.internal.entity.Amenity;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ResourceAmenity;
import com.system.booking.modules.inventory.internal.exception.ResourceNotFoundException;
import com.system.booking.modules.inventory.internal.repository.AmenityRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceAmenityRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import jakarta.persistence.EntityNotFoundException;
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
        Amenity amenity = Amenity.builder()
                .tenantId(tenantId)
                .name(req.name())
                .description(req.description())
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
                .orElseThrow(() -> new EntityNotFoundException("Amenity not found"));
        return toResponse(amenity);
    }

    @Transactional
    public AmenityResponse updateAmenity(UUID tenantId, UUID amenityId, UpdateAmenityRequest req) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new EntityNotFoundException("Amenity not found"));
        if (req.name() != null) amenity.setName(req.name());
        if (req.description() != null) amenity.setDescription(req.description());
        if (req.isActive() != null) amenity.setIsActive(req.isActive());
        amenity = amenityRepository.save(amenity);
        return toResponse(amenity);
    }

    @Transactional
    public void deleteAmenity(UUID tenantId, UUID amenityId) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new EntityNotFoundException("Amenity not found"));
        amenityRepository.delete(amenity);
    }

    @Transactional
    public void linkAmenityToResource(UUID tenantId, UUID resourceId, UUID amenityId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new EntityNotFoundException("Amenity not found"));

        // Tenant consistency check
        if (!resource.getTenantId().equals(amenity.getTenantId())) {
            throw new IllegalArgumentException(
                    "Resource and Amenity must belong to the same tenant");
        }

        resourceAmenityRepository.findByResourceIdAndAmenityId(resourceId, amenityId)
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
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new EntityNotFoundException("Amenity not found"));
        resourceAmenityRepository.deleteByResourceIdAndAmenityId(resourceId, amenityId);
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> listAmenitiesForResource(UUID resourceId) {
        return resourceAmenityRepository.findByResourceId(resourceId).stream()
                .map(link -> toResponse(link.getAmenity()))
                .collect(Collectors.toList());
    }

    private AmenityResponse toResponse(Amenity a) {
        return new AmenityResponse(
                a.getId(), a.getTenantId(), a.getName(),
                a.getDescription(), a.getIsActive(), a.getCreatedAt()
        );
    }
}
