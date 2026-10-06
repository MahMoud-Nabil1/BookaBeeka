package com.system.booking.config;

import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ResourceServiceLink;
import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceServiceLinkRepository;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Development-only controller to manually trigger data seeding.
 * Only available when 'dev' profile is active.
 */
@RestController
@RequestMapping("/api/dev/seed")
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataSeederController {

    private final TenantRepository tenantRepository;
    private final ResourceRepository resourceRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ResourceServiceLinkRepository resourceServiceLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final OwnerRepository ownerRepository;

    /**
     * Generate BCrypt hash for a password.
     * Useful for debugging password authentication issues.
     * Example: GET /api/dev/seed/generate-hash?password=superadmin123
     */
    @GetMapping("/generate-hash")
    public ResponseEntity<Map<String, String>> generatePasswordHash(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        log.info("Generated BCrypt hash for password: {}", password);
        return ResponseEntity.ok(Map.of(
            "password", password,
            "hash", hash,
            "algorithm", "BCrypt",
            "note", "Use this hash in your SQL UPDATE statement"
        ));
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> seedAllTenants() {
        log.info("Manual seed triggered via API");
        
        List<Tenant> tenants = tenantRepository.findAll();
        if (tenants.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No tenants found. Register an owner first."));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Tenant tenant : tenants) {
            UUID tenantId = tenant.getId();
            long existingResources = resourceRepository.countByTenantId(tenantId);
            
            if (existingResources > 0) {
                results.add(Map.of(
                    "tenantId", tenantId.toString(),
                    "tenantName", tenant.getName(),
                    "status", "skipped",
                    "reason", "Already has " + existingResources + " resources"
                ));
                continue;
            }
            
            try {
                seedTenantData(tenantId);
                long newCount = resourceRepository.countByTenantId(tenantId);
                results.add(Map.of(
                    "tenantId", tenantId.toString(),
                    "tenantName", tenant.getName(),
                    "status", "success",
                    "resourcesCreated", newCount
                ));
            } catch (Exception e) {
                log.error("Error seeding tenant {}: {}", tenant.getName(), e.getMessage(), e);
                results.add(Map.of(
                    "tenantId", tenantId.toString(),
                    "tenantName", tenant.getName(),
                    "status", "error",
                    "error", e.getMessage()
                ));
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Seeding completed",
            "results", results
        ));
    }

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<Map<String, Object>> seedSpecificTenant(@PathVariable UUID tenantId) {
        log.info("Manual seed triggered for tenant: {}", tenantId);
        
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Tenant tenant = tenantOpt.get();
        long existingResources = resourceRepository.countByTenantId(tenantId);
        
        if (existingResources > 0) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Tenant already has " + existingResources + " resources",
                    "tenantName", tenant.getName()
                ));
        }
        
        try {
            seedTenantData(tenantId);
            long newCount = resourceRepository.countByTenantId(tenantId);
            return ResponseEntity.ok(Map.of(
                "message", "Seeding completed successfully",
                "tenantId", tenantId.toString(),
                "tenantName", tenant.getName(),
                "resourcesCreated", newCount
            ));
        } catch (Exception e) {
            log.error("Error seeding tenant {}: {}", tenant.getName(), e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tenant/{tenantId}/resources")
    public ResponseEntity<Map<String, Object>> clearTenantResources(@PathVariable UUID tenantId) {
        log.info("Clearing resources for tenant: {}", tenantId);
        
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Tenant tenant = tenantOpt.get();
        
        // Delete in order: links -> resources, service offerings
        resourceServiceLinkRepository.deleteByTenantId(tenantId);
        long resourceCount = resourceRepository.countByTenantId(tenantId);
        resourceRepository.deleteByTenantId(tenantId);
        
        long serviceCount = serviceOfferingRepository.countByTenantId(tenantId);
        serviceOfferingRepository.deleteByTenantId(tenantId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Resources cleared",
            "tenantName", tenant.getName(),
            "resourcesDeleted", resourceCount,
            "servicesDeleted", serviceCount
        ));
    }

    private void seedTenantData(UUID tenantId) {
        // Create rooms
        Resource room1 = createRoom(tenantId, "Deluxe Suite", "SUITE", 2, new BigDecimal("199.99"),
            Map.of("view", "Ocean View", "bedType", "King", "amenities", 
                List.of("Mini Bar", "Balcony", "Jacuzzi")));
        
        Resource room2 = createRoom(tenantId, "Standard Room", "STANDARD", 2, new BigDecimal("99.99"),
            Map.of("view", "City View", "bedType", "Queen", "amenities", 
                List.of("WiFi", "TV", "Air Conditioning")));
        
        Resource room3 = createRoom(tenantId, "Family Suite", "FAMILY", 4, new BigDecimal("249.99"),
            Map.of("view", "Garden View", "bedType", "2 Queen Beds", "amenities", 
                List.of("Kitchen", "Living Room", "Washer/Dryer")));
        
        Resource room4 = createRoom(tenantId, "Presidential Suite", "PRESIDENTIAL", 4, new BigDecimal("499.99"),
            Map.of("view", "Panoramic Ocean View", "bedType", "King + Queen", "amenities", 
                List.of("Private Pool", "Butler Service", "Premium Bar", "Home Theater")));

        Resource room5 = createRoom(tenantId, "Economy Room", "ECONOMY", 1, new BigDecimal("59.99"),
            Map.of("view", "Courtyard View", "bedType", "Twin", "amenities", 
                List.of("WiFi", "Desk")));

        resourceRepository.saveAll(List.of(room1, room2, room3, room4, room5));
        log.info("Created 5 rooms for tenant {}", tenantId);

        // Create service offerings
        ServiceOffering nightly = createServiceOffering(tenantId, 
            "Nightly Stay", new BigDecimal("199.99"), 1440, 60);
        ServiceOffering weekly = createServiceOffering(tenantId,
            "Weekly Stay (7 nights)", new BigDecimal("1199.99"), 10080, 60);
        ServiceOffering hourly = createServiceOffering(tenantId,
            "Hourly Booking", new BigDecimal("29.99"), 60, 15);
        ServiceOffering luxury = createServiceOffering(tenantId,
            "Luxury Package", new BigDecimal("499.99"), 1440, 120);

        serviceOfferingRepository.saveAll(List.of(nightly, weekly, hourly, luxury));
        log.info("Created 4 service offerings for tenant {}", tenantId);

        // Link services to resources
        linkServiceToResource(tenantId, room1, nightly);
        linkServiceToResource(tenantId, room1, weekly);
        linkServiceToResource(tenantId, room1, luxury);
        linkServiceToResource(tenantId, room2, nightly);
        linkServiceToResource(tenantId, room2, weekly);
        linkServiceToResource(tenantId, room2, hourly);
        linkServiceToResource(tenantId, room3, nightly);
        linkServiceToResource(tenantId, room3, weekly);
        linkServiceToResource(tenantId, room4, luxury);
        linkServiceToResource(tenantId, room4, weekly);
        linkServiceToResource(tenantId, room5, nightly);
        linkServiceToResource(tenantId, room5, hourly);

        log.info("Linked services to resources for tenant {}", tenantId);
    }

    private Resource createRoom(UUID tenantId, String name, String resourceType, 
                                int capacity, BigDecimal pricePerNight, Map<String, Object> specs) {
        return Resource.builder()
                .tenantId(tenantId)
                .name(name)
                .resourceType(resourceType)
                .capacity(capacity)
                .pricePerNight(pricePerNight)
                .currency("USD")
                .specs(specs)
                .isActive(true)
                .isBookable(true)
                .build();
    }

    private ServiceOffering createServiceOffering(UUID tenantId, String name, 
                                                 BigDecimal price, int durationMinutes, int bufferMinutes) {
        return ServiceOffering.builder()
                .tenantId(tenantId)
                .name(name)
                .price(price)
                .durationMinutes(durationMinutes)
                .bufferMinutes(bufferMinutes)
                .customAttributes(new HashMap<>())
                .isActive(true)
                .build();
    }

    private void linkServiceToResource(UUID tenantId, Resource resource, ServiceOffering service) {
        ResourceServiceLink link = ResourceServiceLink.builder()
                .tenantId(tenantId)
                .resource(resource)
                .serviceOffering(service)
                .build();
        resourceServiceLinkRepository.save(link);
    }
}
