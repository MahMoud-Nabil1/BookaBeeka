package com.system.booking.modules.availability.api;

import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityModuleApi availabilityModuleApi;
    private final EntityManager entityManager;

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDto>> getSlots(
            @RequestParam UUID tenantId,
            @RequestParam UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            
        List<SlotDto> slots = availabilityModuleApi.getAvailableSlots(tenantId, resourceId, date);
        return ResponseEntity.ok(slots);
    }

    /**
     * A helper endpoint just for you to quickly seed some test data!
     * It creates a dummy Tenant, Resource, and Schedule Rule.
     */
    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<Map<String, Object>> seedTestData() {
        // Create dummy tenant
        Tenant tenant = Tenant.builder()
                .name("Test Tenant")
                .subdomain("test-" + UUID.randomUUID().toString().substring(0, 8))
                .status("ACTIVE")
                .build();
        entityManager.persist(tenant);

        // Create dummy branch
        com.system.booking.modules.tenant.internal.entity.Branch branch = com.system.booking.modules.tenant.internal.entity.Branch.builder()
                .tenantId(tenant.getId())
                .name("Main Branch")
                .status("ACTIVE")
                .build();
        entityManager.persist(branch);

        // Create dummy resource
        Resource resource = Resource.builder()
                .tenantId(tenant.getId())
                .branch(branch)
                .name("Test Resource")
                .resourceType("TEST_TYPE")
                .isActive(true)
                .isBookable(true)
                .build();
        entityManager.persist(resource);

        // Create a schedule rule for tomorrow (9 AM to 5 PM)
        short tomorrowDayOfWeek = (short) LocalDate.now().plusDays(1).getDayOfWeek().getValue();
        ScheduleRuleDto rule = new ScheduleRuleDto(
                tomorrowDayOfWeek, 
                LocalTime.of(9, 0), 
                LocalTime.of(17, 0)
        );
        
        availabilityModuleApi.defineScheduleRule(tenant.getId(), resource.getId(), rule);

        return ResponseEntity.ok(Map.of(
                "message", "Test data seeded successfully for tomorrow!",
                "tenantId", tenant.getId(),
                "resourceId", resource.getId(),
                "testDate", LocalDate.now().plusDays(1)
        ));
    }
}
