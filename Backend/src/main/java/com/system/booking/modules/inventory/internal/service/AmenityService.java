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

/**
 * Service responsible for CRUD operations on {@link Amenity} entities and the
 * {@link ResourceAmenity} join records that link amenities to bookable resources.
 *
 * <p><b>Multi-Tenancy Isolation Pattern:</b><br>
 * Every public method accepts a {@code tenantId} extracted from the JWT by the controller.
 * All repository queries use compound {@code (tenantId, id)} lookups, meaning callers can
 * only access amenities owned by their tenant.</p>
 *
 * <p><b>Explicit Cross-Tenant Guard in {@link #linkAmenityToResource}:</b><br>
 * When linking an amenity to a resource, both entities are fetched using the caller's
 * {@code tenantId}. An additional explicit guard then re-validates tenant membership on
 * both resolved entities. This dual-check ensures that even if a repository bypass were
 * somehow introduced in the future, the service layer would still reject cross-tenant links.</p>
 */
@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final ResourceAmenityRepository resourceAmenityRepository;
    private final ResourceRepository resourceRepository;

    /**
     * Creates a new amenity scoped to the given tenant.
     *
     * <p>Performs an application-level duplicate check (case-insensitive, tenant-scoped)
     * before persisting, mirroring the {@code uk_amenity_tenant_name} DB constraint
     * with a user-friendly error message.</p>
     */
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

    /**
     * Lists all amenities belonging to the given tenant.
     */
    @Transactional(readOnly = true)
    public List<AmenityResponse> listAmenities(UUID tenantId) {
        return amenityRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single amenity by ID, scoped to the given tenant.
     *
     * <p>The compound lookup returns 404 for cross-tenant access attempts.</p>
     */
    @Transactional(readOnly = true)
    public AmenityResponse getAmenity(UUID tenantId, UUID amenityId) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        return toResponse(amenity);
    }

    /**
     * Updates an amenity, scoped to the given tenant.
     */
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

    /**
     * Deletes an amenity, scoped to the given tenant.
     */
    @Transactional
    public void deleteAmenity(UUID tenantId, UUID amenityId) {
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        amenityRepository.delete(amenity);
    }

    /**
     * Links an amenity to a resource, enforcing strict tenant isolation on both entities.
     *
     * <p>Both the Resource and the Amenity are resolved using the caller's {@code tenantId},
     * which already filters out cross-tenant entities at the repository level. The explicit
     * guard below ({@code !resource.getTenantId().equals(tenantId)}) provides a second layer
     * of defence-in-depth — it would catch any future refactoring that accidentally removes
     * the tenant predicate from the repository queries, ensuring cross-tenant links are
     * categorically impossible.</p>
     *
     * <p>The link creation is idempotent: if a link already exists, it is returned without
     * creating a duplicate row, preventing constraint violations.</p>
     */
    @Transactional
    public void linkAmenityToResource(UUID tenantId, UUID resourceId, UUID amenityId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        Amenity amenity = amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));

        // Explicit cross-tenant guard — defence-in-depth to ensure that neither the resource
        // nor the amenity belongs to a different tenant, even if the repository query is somehow bypassed.
        if (!resource.getTenantId().equals(tenantId) || !amenity.getTenantId().equals(tenantId)) {
            throw new CrossTenantViolationException("Resource and Amenity must belong to the authenticated tenant");
        }

        // Idempotent upsert: skip saving if the link already exists.
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

    /**
     * Unlinks an amenity from a resource, scoped to the given tenant.
     *
     * <p>Validates both entities belong to the caller's tenant before deletion.</p>
     */
    @Transactional
    public void unlinkAmenityFromResource(UUID tenantId, UUID resourceId, UUID amenityId) {
        resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        amenityRepository.findByTenantIdAndId(tenantId, amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        resourceAmenityRepository.deleteByTenantIdAndResourceIdAndAmenityId(tenantId, resourceId, amenityId);
    }

    /**
     * Lists all amenities linked to a given resource, scoped to the caller's tenant.
     *
     * <p>Verifies the resource exists and belongs to the tenant before querying
     * the join table, preventing data leakage through the listing endpoint.</p>
     */
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