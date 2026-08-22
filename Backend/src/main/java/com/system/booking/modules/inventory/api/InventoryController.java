package com.system.booking.modules.inventory.api;

import com.system.booking.modules.inventory.internal.dto.*;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ResourceServiceLink;
import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import com.system.booking.modules.inventory.internal.exception.ResourceNotFoundException;
import com.system.booking.modules.inventory.internal.exception.ServiceOfferingNotFoundException;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceServiceLinkRepository;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import com.system.booking.modules.inventory.internal.service.ResourceService;
import com.system.booking.modules.inventory.internal.service.ServiceOfferingService;
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
    private final ServiceOfferingService serviceOfferingService;
    private final ResourceServiceLinkRepository resourceServiceLinkRepository;
    private final ResourceRepository resourceRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    @PostMapping("/resources")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(req));
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            @RequestParam UUID tenantId,
            @PathVariable UUID id,
            @RequestBody UpdateResourceRequest req) {
        return ResponseEntity.ok(resourceService.updateResource(tenantId, id, req));
    }

    @GetMapping("/resources")
    public ResponseEntity<List<ResourceResponse>> listResources(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(resourceService.listResources(tenantId));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ResourceResponse> getResource(
            @RequestParam UUID tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(resourceService.getResource(tenantId, id));
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> deleteResource(
            @RequestParam UUID tenantId,
            @PathVariable UUID id) {
        resourceService.deleteResource(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceOfferingResponse> createServiceOffering(@Valid @RequestBody CreateServiceOfferingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOfferingService.createServiceOffering(req));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ServiceOfferingResponse> updateServiceOffering(
            @RequestParam UUID tenantId,
            @PathVariable UUID id,
            @RequestBody UpdateServiceOfferingRequest req) {
        return ResponseEntity.ok(serviceOfferingService.updateServiceOffering(tenantId, id, req));
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceOfferingResponse>> listServiceOfferings(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(serviceOfferingService.listServiceOfferings(tenantId));
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceOfferingResponse> getServiceOffering(
            @RequestParam UUID tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(serviceOfferingService.getServiceOffering(tenantId, id));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteServiceOffering(
            @RequestParam UUID tenantId,
            @PathVariable UUID id) {
        serviceOfferingService.deleteServiceOffering(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PostMapping("/resources/{resourceId}/link-service")
    public ResponseEntity<Void> linkServiceToResource(
            @PathVariable UUID resourceId,
            @RequestParam UUID tenantId,
            @RequestParam UUID serviceOfferingId) {
        
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        ServiceOffering serviceOffering = serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));

        resourceServiceLinkRepository.findByResourceIdAndServiceOfferingId(resourceId, serviceOfferingId)
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
    @DeleteMapping("/resources/{resourceId}/unlink-service")
    public ResponseEntity<Void> unlinkServiceFromResource(
            @PathVariable UUID resourceId,
            @RequestParam UUID tenantId,
            @RequestParam UUID serviceOfferingId) {
        
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        ServiceOffering serviceOffering = serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException("Service offering not found"));

        resourceServiceLinkRepository.deleteByResourceIdAndServiceOfferingId(resource.getId(), serviceOffering.getId());
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    @GetMapping("/resources/{resourceId}/services")
    public ResponseEntity<List<ServiceOfferingResponse>> listLinkedServices(
            @PathVariable UUID resourceId,
            @RequestParam UUID tenantId) {
            
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
                
        List<ServiceOfferingResponse> linkedServices = resourceServiceLinkRepository.findByResourceId(resource.getId())
                .stream()
                .map(link -> {
                    ServiceOffering o = link.getServiceOffering();
                    return new ServiceOfferingResponse(
                        o.getId(), o.getTenantId(), o.getBranch() != null ? o.getBranch().getId() : null,
                        o.getName(), o.getPrice(), o.getDurationMinutes(), o.getBufferMinutes(),
                        o.getCustomAttributes(), o.getIsActive(), o.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(linkedServices);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Resource not found", "message", e.getMessage()));
    }

    @ExceptionHandler(ServiceOfferingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleServiceOfferingNotFound(ServiceOfferingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Service offering not found", "message", e.getMessage()));
    }
}
