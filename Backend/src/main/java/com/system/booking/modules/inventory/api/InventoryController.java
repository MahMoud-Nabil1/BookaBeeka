package com.system.booking.modules.inventory.api;

import com.system.booking.modules.inventory.internal.dto.request.*;
import com.system.booking.modules.inventory.internal.dto.response.*;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ResourceServiceLink;
import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import com.system.booking.modules.inventory.internal.exception.*;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceServiceLinkRepository;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import com.system.booking.modules.inventory.internal.service.AmenityService;
import com.system.booking.modules.inventory.internal.service.ResourceService;
import com.system.booking.modules.inventory.internal.service.RoomTypeService;
import com.system.booking.modules.inventory.internal.service.ServiceOfferingService;
import com.system.booking.modules.security.context.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ResourceService resourceService;
    private final RoomTypeService roomTypeService;
    private final ServiceOfferingService serviceOfferingService;
    private final AmenityService amenityService;
    private final ResourceServiceLinkRepository resourceServiceLinkRepository;
    private final ResourceRepository resourceRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    private UUID getTenantId() {
        return TenantContextHolder.getRequiredContext().tenantId();
    }

    // ── RoomType Endpoints ──

    @PostMapping("/room-types")
    public ResponseEntity<RoomTypeResponse> createRoomType(@Valid @RequestBody RoomTypeCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomTypeService.createRoomType(getTenantId(), req));
    }

    @GetMapping("/room-types")
    public ResponseEntity<List<RoomTypeResponse>> listRoomTypes() {
        return ResponseEntity.ok(roomTypeService.listRoomTypes(getTenantId()));
    }

    @GetMapping("/room-types/branch/{branchId}")
    public ResponseEntity<List<RoomTypeResponse>> listRoomTypesByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(roomTypeService.listRoomTypesByBranch(getTenantId(), branchId));
    }

    @GetMapping("/room-types/{id}")
    public ResponseEntity<RoomTypeResponse> getRoomType(@PathVariable UUID id) {
        return ResponseEntity.ok(roomTypeService.getRoomType(getTenantId(), id));
    }

    @PutMapping("/room-types/{id}")
    public ResponseEntity<RoomTypeResponse> updateRoomType(
            @PathVariable UUID id,
            @Valid @RequestBody RoomTypeUpdateRequest req) {
        return ResponseEntity.ok(roomTypeService.updateRoomType(getTenantId(), id, req));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<Void> deleteRoomType(@PathVariable UUID id) {
        roomTypeService.deleteRoomType(getTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Resource / Room Endpoints ──

    @PostMapping("/resources")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(getTenantId(), req));
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable UUID id,
            @RequestBody UpdateResourceRequest req) {
        return ResponseEntity.ok(resourceService.updateResource(getTenantId(), id, req));
    }

    @GetMapping("/resources")
    public ResponseEntity<List<ResourceResponse>> listResources() {
        return ResponseEntity.ok(resourceService.listResources(getTenantId()));
    }

    @GetMapping("/resources/branch/{branchId}")
    public ResponseEntity<List<ResourceResponse>> listResourcesByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(resourceService.listResourcesByBranch(getTenantId(), branchId));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ResourceResponse> getResource(@PathVariable UUID id) {
        return ResponseEntity.ok(resourceService.getResource(getTenantId(), id));
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable UUID id) {
        resourceService.deleteResource(getTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Hotel Services Endpoints ──

    @PostMapping("/services")
    public ResponseEntity<ServiceOfferingResponse> createServiceOffering(@Valid @RequestBody CreateServiceOfferingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOfferingService.createServiceOffering(getTenantId(), req));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ServiceOfferingResponse> updateServiceOffering(
            @PathVariable UUID id,
            @RequestBody UpdateServiceOfferingRequest req) {
        return ResponseEntity.ok(serviceOfferingService.updateServiceOffering(getTenantId(), id, req));
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceOfferingResponse>> listServiceOfferings() {
        return ResponseEntity.ok(serviceOfferingService.listServiceOfferings(getTenantId()));
    }

    @GetMapping("/services/branch/{branchId}")
    public ResponseEntity<List<ServiceOfferingResponse>> listServiceOfferingsByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(serviceOfferingService.listServiceOfferingsByBranch(getTenantId(), branchId));
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceOfferingResponse> getServiceOffering(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceOfferingService.getServiceOffering(getTenantId(), id));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteServiceOffering(@PathVariable UUID id) {
        serviceOfferingService.deleteServiceOffering(getTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Resource <-> Service Linking ──

    @Transactional
    @PostMapping("/resources/{resourceId}/link-service/{serviceOfferingId}")
    public ResponseEntity<Void> linkServiceToResource(
            @PathVariable UUID resourceId,
            @PathVariable UUID serviceOfferingId) {

        UUID tenantId = getTenantId();
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        ServiceOffering serviceOffering = serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException(serviceOfferingId));

        resourceServiceLinkRepository.findByTenantIdAndResourceIdAndServiceOfferingId(tenantId, resourceId, serviceOfferingId)
                .orElseGet(() -> {
                    ResourceServiceLink link = ResourceServiceLink.builder()
                            .tenantId(tenantId)
                            .resource(resource)
                            .serviceOffering(serviceOffering)
                            .build();
                    return resourceServiceLinkRepository.save(link);
                });

        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/resources/{resourceId}/unlink-service/{serviceOfferingId}")
    public ResponseEntity<Void> unlinkServiceFromResource(
            @PathVariable UUID resourceId,
            @PathVariable UUID serviceOfferingId) {

        UUID tenantId = getTenantId();
        resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException(serviceOfferingId));

        resourceServiceLinkRepository.deleteByTenantIdAndResourceIdAndServiceOfferingId(tenantId, resourceId, serviceOfferingId);
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    @GetMapping("/resources/{resourceId}/services")
    public ResponseEntity<List<ServiceOfferingResponse>> listLinkedServices(@PathVariable UUID resourceId) {
        UUID tenantId = getTenantId();
        resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        List<ServiceOfferingResponse> linkedServices = resourceServiceLinkRepository.findByTenantIdAndResourceId(tenantId, resourceId)
                .stream()
                .map(link -> {
                    ServiceOffering o = link.getServiceOffering();
                    return new ServiceOfferingResponse(
                            o.getId(), o.getTenantId(), o.getBranch() != null ? o.getBranch().getId() : null,
                            o.getName(), o.getDescription(), o.getPrice(), o.getDurationMinutes(), o.getBufferMinutes(),
                            o.getCustomAttributes(), o.getIsActive(),
                            o.getCreatedAt() != null ? o.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(linkedServices);
    }

    // ── Amenity Endpoints ──

    @PostMapping("/amenities")
    public ResponseEntity<AmenityResponse> createAmenity(@Valid @RequestBody CreateAmenityRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amenityService.createAmenity(getTenantId(), req));
    }

    @GetMapping("/amenities")
    public ResponseEntity<List<AmenityResponse>> listAmenities() {
        return ResponseEntity.ok(amenityService.listAmenities(getTenantId()));
    }

    @GetMapping("/amenities/{id}")
    public ResponseEntity<AmenityResponse> getAmenity(@PathVariable UUID id) {
        return ResponseEntity.ok(amenityService.getAmenity(getTenantId(), id));
    }

    @PutMapping("/amenities/{id}")
    public ResponseEntity<AmenityResponse> updateAmenity(
            @PathVariable UUID id,
            @RequestBody UpdateAmenityRequest req) {
        return ResponseEntity.ok(amenityService.updateAmenity(getTenantId(), id, req));
    }

    @DeleteMapping("/amenities/{id}")
    public ResponseEntity<Void> deleteAmenity(@PathVariable UUID id) {
        amenityService.deleteAmenity(getTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Resource <-> Amenity Linking ──

    @PostMapping("/resources/{resourceId}/amenities/{amenityId}")
    public ResponseEntity<Void> linkAmenity(
            @PathVariable UUID resourceId,
            @PathVariable UUID amenityId) {
        amenityService.linkAmenityToResource(getTenantId(), resourceId, amenityId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/resources/{resourceId}/amenities/{amenityId}")
    public ResponseEntity<Void> unlinkAmenity(
            @PathVariable UUID resourceId,
            @PathVariable UUID amenityId) {
        amenityService.unlinkAmenityFromResource(getTenantId(), resourceId, amenityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resources/{resourceId}/amenities")
    public ResponseEntity<List<AmenityResponse>> listResourceAmenities(@PathVariable UUID resourceId) {
        return ResponseEntity.ok(amenityService.listAmenitiesForResource(getTenantId(), resourceId));
    }

    // ── Exception Handlers ──

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Resource not found", "message", e.getMessage()));
    }

    @ExceptionHandler(RoomTypeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoomTypeNotFound(RoomTypeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Room type not found", "message", e.getMessage()));
    }

    @ExceptionHandler(AmenityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAmenityNotFound(AmenityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Amenity not found", "message", e.getMessage()));
    }

    @ExceptionHandler(ServiceOfferingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleServiceOfferingNotFound(ServiceOfferingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Service offering not found", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateInventoryEntityException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEntity(DuplicateInventoryEntityException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Duplicate resource/conflict", "message", e.getMessage()));
    }

    @ExceptionHandler(CrossTenantViolationException.class)
    public ResponseEntity<Map<String, String>> handleCrossTenantViolation(CrossTenantViolationException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Cross-tenant access violation", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Bad request", "message", e.getMessage()));
    }
}