package com.system.booking.config;

import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import com.system.booking.modules.inventory.internal.entity.ResourceServiceLink;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceServiceLinkRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds initial test data for development/testing environments.
 * Only runs when 'dev' profile is active.
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(
            TenantRepository tenantRepository,
            ResourceRepository resourceRepository,
            ServiceOfferingRepository serviceOfferingRepository,
            ResourceServiceLinkRepository resourceServiceLinkRepository) {
        
        return args -> {
            log.info("DataSeeder: Checking database status...");
            
            // Get all tenants
            List<Tenant> tenants = tenantRepository.findAll();
            if (tenants.isEmpty()) {
                log.warn("DataSeeder: No tenants found. Please create a tenant first by registering an owner.");
                return;
            }

            log.info("DataSeeder: Found {} tenant(s) in database", tenants.size());
            
            // Seed data for each tenant that doesn't have resources yet
            for (Tenant tenant : tenants) {
                UUID tenantId = tenant.getId();
                if (SystemTenantInitializer.SYSTEM_TENANT_ID.equals(tenantId) || "system-platform".equalsIgnoreCase(tenant.getSubdomain())) {
                    continue;
                }
                
                long existingResources = resourceRepository.countByTenantId(tenantId);
                if (existingResources > 0) {
                    log.info("DataSeeder: Tenant '{}' already has {} resources. Skipping.", 
                        tenant.getName(), existingResources);
                    continue;
                }

                log.info("DataSeeder: Starting database seeding for tenant: {} ({})", tenant.getName(), tenantId);
                seedTenantData(tenantId, resourceRepository, serviceOfferingRepository, resourceServiceLinkRepository);
            }
        };
    }

    private void seedTenantData(UUID tenantId, 
                               ResourceRepository resourceRepository,
                               ServiceOfferingRepository serviceOfferingRepository,
                               ResourceServiceLinkRepository resourceServiceLinkRepository) {
        try {
            // Create some rooms (resources)
            Resource room1 = createRoom(tenantId, "Deluxe Suite", "SUITE", 2, 
                Map.of(
                    "view", "Ocean View",
                    "bedType", "King",
                    "amenities", List.of("Mini Bar", "Balcony", "Jacuzzi")
                ));
            
            Resource room2 = createRoom(tenantId, "Standard Room", "STANDARD", 2,
                Map.of(
                    "view", "City View",
                    "bedType", "Queen",
                    "amenities", List.of("WiFi", "TV", "Air Conditioning")
                ));
            
            Resource room3 = createRoom(tenantId, "Family Suite", "FAMILY", 4,
                Map.of(
                    "view", "Garden View",
                    "bedType", "2 Queen Beds",
                    "amenities", List.of("Kitchen", "Living Room", "Washer/Dryer")
                ));
            
            Resource room4 = createRoom(tenantId, "Presidential Suite", "PRESIDENTIAL", 4,
                Map.of(
                    "view", "Panoramic Ocean View",
                    "bedType", "King + Queen",
                    "amenities", List.of("Private Pool", "Butler Service", "Premium Bar", "Home Theater")
                ));

            Resource room5 = createRoom(tenantId, "Economy Room", "ECONOMY", 1,
                Map.of(
                    "view", "Courtyard View",
                    "bedType", "Twin",
                    "amenities", List.of("WiFi", "Desk")
                ));

            resourceRepository.saveAll(List.of(room1, room2, room3, room4, room5));
            log.info("Created {} rooms", 5);

            // Create room types (service offerings)
            ServiceOffering nightly = createRoomType(tenantId, 
                "Nightly Stay", new BigDecimal("199.99"), 1440, 60);
            
            ServiceOffering weekly = createRoomType(tenantId,
                "Weekly Stay (7 nights)", new BigDecimal("1199.99"), 10080, 60);
            
            ServiceOffering hourly = createRoomType(tenantId,
                "Hourly Booking", new BigDecimal("29.99"), 60, 15);
            
            ServiceOffering luxury = createRoomType(tenantId,
                "Luxury Package", new BigDecimal("499.99"), 1440, 120);

            serviceOfferingRepository.saveAll(List.of(nightly, weekly, hourly, luxury));
            log.info("Created {} room types", 4);

            // Link room types to rooms
            linkServiceToResource(tenantId, room1, nightly, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room1, weekly, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room1, luxury, resourceServiceLinkRepository);
            
            linkServiceToResource(tenantId, room2, nightly, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room2, weekly, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room2, hourly, resourceServiceLinkRepository);
            
            linkServiceToResource(tenantId, room3, nightly, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room3, weekly, resourceServiceLinkRepository);
            
            linkServiceToResource(tenantId, room4, luxury, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room4, weekly, resourceServiceLinkRepository);
            
            linkServiceToResource(tenantId, room5, nightly, resourceServiceLinkRepository);
            linkServiceToResource(tenantId, room5, hourly, resourceServiceLinkRepository);

            log.info("Database seeding completed successfully for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Error seeding data for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    private Resource createRoom(UUID tenantId, String name, 
                                String resourceType, int capacity, Map<String, Object> specs) {
        return Resource.builder()
                .tenantId(tenantId)
                .name(name)
                .resourceType(resourceType)
                .capacity(capacity)
                .specs(specs)
                .isActive(true)
                .isBookable(true)
                .build();
    }

    private ServiceOffering createRoomType(UUID tenantId, String name, 
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

    private void linkServiceToResource(UUID tenantId, Resource resource, 
                                      ServiceOffering service, ResourceServiceLinkRepository repo) {
        ResourceServiceLink link = ResourceServiceLink.builder()
                .tenantId(tenantId)
                .resource(resource)
                .serviceOffering(service)
                .build();
        repo.save(link);
    }
}
