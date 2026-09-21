package com.system.booking.config;

import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import com.system.booking.modules.inventory.internal.entity.ResourceServiceLink;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceServiceLinkRepository;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.repository.BranchRepository;
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
            BranchRepository branchRepository,
            ResourceRepository resourceRepository,
            ServiceOfferingRepository serviceOfferingRepository,
            ResourceServiceLinkRepository resourceServiceLinkRepository) {
        
        return args -> {
            // Check if we already have data
            if (resourceRepository.count() > 0) {
                log.info("Database already contains resources. Skipping seed data.");
                return;
            }

            log.info("Starting database seeding...");

            // Get the first branch (you should have at least one tenant/branch set up)
            List<Branch> branches = branchRepository.findAll();
            if (branches.isEmpty()) {
                log.warn("No branches found. Please create a tenant and branch first.");
                return;
            }

            Branch branch = branches.get(0);
            UUID tenantId = branch.getTenantId();

            log.info("Seeding data for tenant: {}, branch: {}", tenantId, branch.getId());

            // Create some rooms (resources)
            Resource room1 = createRoom(tenantId, branch, "Deluxe Suite", "SUITE", 2, 
                Map.of(
                    "view", "Ocean View",
                    "bedType", "King",
                    "amenities", List.of("Mini Bar", "Balcony", "Jacuzzi")
                ));
            
            Resource room2 = createRoom(tenantId, branch, "Standard Room", "STANDARD", 2,
                Map.of(
                    "view", "City View",
                    "bedType", "Queen",
                    "amenities", List.of("WiFi", "TV", "Air Conditioning")
                ));
            
            Resource room3 = createRoom(tenantId, branch, "Family Suite", "FAMILY", 4,
                Map.of(
                    "view", "Garden View",
                    "bedType", "2 Queen Beds",
                    "amenities", List.of("Kitchen", "Living Room", "Washer/Dryer")
                ));
            
            Resource room4 = createRoom(tenantId, branch, "Presidential Suite", "PRESIDENTIAL", 4,
                Map.of(
                    "view", "Panoramic Ocean View",
                    "bedType", "King + Queen",
                    "amenities", List.of("Private Pool", "Butler Service", "Premium Bar", "Home Theater")
                ));

            Resource room5 = createRoom(tenantId, branch, "Economy Room", "ECONOMY", 1,
                Map.of(
                    "view", "Courtyard View",
                    "bedType", "Twin",
                    "amenities", List.of("WiFi", "Desk")
                ));

            resourceRepository.saveAll(List.of(room1, room2, room3, room4, room5));
            log.info("Created {} rooms", 5);

            // Create room types (service offerings)
            ServiceOffering nightly = createRoomType(tenantId, branch, 
                "Nightly Stay", new BigDecimal("199.99"), 1440, 60);
            
            ServiceOffering weekly = createRoomType(tenantId, branch,
                "Weekly Stay (7 nights)", new BigDecimal("1199.99"), 10080, 60);
            
            ServiceOffering hourly = createRoomType(tenantId, branch,
                "Hourly Booking", new BigDecimal("29.99"), 60, 15);
            
            ServiceOffering luxury = createRoomType(tenantId, branch,
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

            log.info("Database seeding completed successfully!");
        };
    }

    private Resource createRoom(UUID tenantId, Branch branch, String name, 
                                String resourceType, int capacity, Map<String, Object> specs) {
        return Resource.builder()
                .tenantId(tenantId)
                .branch(branch)
                .name(name)
                .resourceType(resourceType)
                .capacity(capacity)
                .specs(specs)
                .isActive(true)
                .isBookable(true)
                .build();
    }

    private ServiceOffering createRoomType(UUID tenantId, Branch branch, String name, 
                                          BigDecimal price, int durationMinutes, int bufferMinutes) {
        return ServiceOffering.builder()
                .tenantId(tenantId)
                .branch(branch)
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
