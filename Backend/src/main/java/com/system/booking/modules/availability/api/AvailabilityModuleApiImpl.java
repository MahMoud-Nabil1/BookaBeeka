package com.system.booking.modules.availability.api;

import com.system.booking.modules.availability.api.dto.AvailableRoomResponse;
import com.system.booking.modules.availability.api.dto.HotelInfo;
import com.system.booking.modules.availability.api.dto.PricingInfo;
import com.system.booking.modules.availability.api.dto.RoomInfo;
import com.system.booking.modules.availability.api.dto.RoomSearchRequest;
import com.system.booking.modules.availability.api.dto.StayInfo;
import com.system.booking.modules.availability.internal.repository.RoomAvailabilityRepository;
import com.system.booking.modules.availability.internal.service.AvailabilityExceptionService;
import com.system.booking.modules.availability.internal.service.ScheduleRuleService;
import com.system.booking.modules.availability.internal.service.SlotGenerationService;
import com.system.booking.modules.availability.internal.service.SlotLockingService;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.dto.response.ServiceOfferingResponse;
import com.system.booking.modules.inventory.internal.repository.ResourceAmenityRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityModuleApiImpl implements AvailabilityModuleApi {
    private final ScheduleRuleService scheduleRuleService;
    private final AvailabilityExceptionService exceptionService;
    private final SlotGenerationService slotGenerationService;
    private final SlotLockingService slotLockingService;
    private final EntityManager entityManager;
    private final InventoryModuleApi inventoryApi;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final ResourceAmenityRepository resourceAmenityRepository;

    // ── Legacy slot-based methods (untouched) ────────────────────

    @Override
    public List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, LocalDate date) {
        return slotGenerationService.generateSlots(tenantId, resourceId, date, 60, 0);
    }
    
    @Override
    public List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, UUID serviceOfferingId, LocalDate date) {
        ServiceOfferingResponse service = inventoryApi.getServiceOfferingByTenantAndId(tenantId, serviceOfferingId);
        int duration = service.durationMinutes();
        int buffer = service.bufferMinutes() != null ? service.bufferMinutes() : 0;
        return slotGenerationService.generateSlots(tenantId, resourceId, date, duration, buffer);
    }
    
    @Override
    public SlotLockDto lockSlot(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end, UUID userId) {
        Resource resource = entityManager.getReference(Resource.class, resourceId);
        return slotLockingService.acquireTemporaryLock(tenantId, resource, start, end, userId);
    }
    @Override
    public void releaseLock(UUID tenantId, UUID lockId) { slotLockingService.releaseLock(tenantId, lockId); }
    @Override
    public void consumeLock(UUID tenantId, UUID lockId, UUID bookingId) { slotLockingService.consumeLock(tenantId, lockId, bookingId); }
    @Override
    public boolean isRangeAvailable(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end) {
        LocalDate date = start.toLocalDate();
        List<SlotDto> openSlots = slotGenerationService.generateSlots(tenantId, resourceId, date, 60, 0);
        return openSlots.stream().anyMatch(slot -> !slot.start().isAfter(start) && !slot.end().isBefore(end));
    }
    @Override
    public void defineScheduleRule(UUID tenantId, UUID resourceId, ScheduleRuleDto rule) {
        Resource resource = entityManager.getReference(Resource.class, resourceId);
        scheduleRuleService.defineScheduleRule(tenantId, resource, rule);
    }
    
    @Override
    public void updateScheduleRule(UUID tenantId, UUID ruleId, ScheduleRuleDto rule) {
        scheduleRuleService.updateScheduleRule(tenantId, ruleId, rule);
    }

    @Override
    public void deleteScheduleRule(UUID tenantId, UUID ruleId) {
        scheduleRuleService.deleteScheduleRule(tenantId, ruleId);
    }
    
    @Override
    public void addAvailabilityException(UUID tenantId, UUID resourceId, ExceptionDto exception) {
        Resource resource = entityManager.getReference(Resource.class, resourceId);
        exceptionService.addAvailabilityException(tenantId, resource, exception);
    }
    
    @Override
    public void updateAvailabilityException(UUID tenantId, UUID exceptionId, ExceptionDto exception) {
        exceptionService.updateException(tenantId, exceptionId, exception);
    }

    @Override
    public void deleteAvailabilityException(UUID tenantId, UUID exceptionId) {
        exceptionService.deleteException(tenantId, exceptionId);
    }

    // ── Hotel date-range availability ────────────────────────────

    @Override
    public Page<AvailableRoomResponse> searchAvailableRooms(RoomSearchRequest request, Pageable pageable) {
        // Normalize amenity IDs to distinct values
        List<UUID> amenityIds = (request.amenities() != null)
                ? request.amenities().stream().distinct().collect(Collectors.toList())
                : null;

        Page<Object[]> rawResults = roomAvailabilityRepository.searchAvailableRooms(
                request.hotelId(),
                request.checkIn(),
                request.checkOut(),
                request.roomType(),
                request.bedType(),
                request.minCapacity(),
                request.minPrice(),
                request.maxPrice(),
                amenityIds,
                pageable
        );

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());

        return rawResults.map(row -> mapToResponse(row, request.checkIn(), request.checkOut(), nights));
    }

    @Override
    public boolean isRoomAvailableForDates(UUID resourceId, LocalDate checkIn, LocalDate checkOut) {
        return roomAvailabilityRepository.isRoomAvailable(resourceId, checkIn, checkOut);
    }

    private AvailableRoomResponse mapToResponse(Object[] row, LocalDate checkIn, LocalDate checkOut, long nights) {
        UUID roomId = (UUID) row[0];
        String roomName = (String) row[1];
        String resourceType = (String) row[2];
        Integer capacity = row[3] != null ? ((Number) row[3]).intValue() : null;
        // row[4] = specs (String/PGobject) — pass as-is for now
        @SuppressWarnings("unchecked")
        Map<String, Object> specs = null; // JSONB returned as string; parse if needed
        BigDecimal pricePerNight = row[5] != null ? new BigDecimal(row[5].toString()) : null;
        String currency = (String) row[6];
        UUID hotelId = (UUID) row[7];
        String hotelName = (String) row[8];
        String subdomain = (String) row[9];

        // Extract bedType from specs if available
        String bedType = null;
        if (row[4] != null) {
            String specsStr = row[4].toString();
            // Simple JSON parsing for bedType
            if (specsStr.contains("\"bedType\"")) {
                int idx = specsStr.indexOf("\"bedType\"");
                int start = specsStr.indexOf("\"", idx + 10) + 1;
                int end = specsStr.indexOf("\"", start);
                if (start > 0 && end > start) {
                    bedType = specsStr.substring(start, end);
                }
            }
        }

        // Fetch amenity names for this room
        List<String> amenityNames = resourceAmenityRepository.findByResourceId(roomId).stream()
                .map(link -> link.getAmenity().getName())
                .collect(Collectors.toList());

        BigDecimal totalPrice = (pricePerNight != null && nights > 0)
                ? pricePerNight.multiply(BigDecimal.valueOf(nights))
                : null;

        return new AvailableRoomResponse(
                new HotelInfo(hotelId, hotelName, subdomain),
                new RoomInfo(roomId, resourceType, capacity, bedType, amenityNames, specs),
                new StayInfo(checkIn, checkOut, (int) nights),
                new PricingInfo(pricePerNight, totalPrice, currency)
        );
    }
}

