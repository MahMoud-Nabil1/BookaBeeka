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
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.dto.response.ServiceOfferingResponse;
import com.system.booking.modules.inventory.internal.entity.Resource;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    // ── Legacy slot-based methods (untouched) ──────────────────────────────────

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
    public void releaseLock(UUID tenantId, UUID lockId) {
        slotLockingService.releaseLock(tenantId, lockId);
    }

    @Override
    public void consumeLock(UUID tenantId, UUID lockId, UUID bookingId) {
        slotLockingService.consumeLock(tenantId, lockId, bookingId);
    }

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

    // ── Hotel date-range availability ──────────────────────────────────────────

    /**
     * Searches available rooms and maps the result page to {@link AvailableRoomResponse}.
     *
     * <p><b>N+1 fix:</b> instead of fetching amenities and photos once per room (which
     * produced 40–120 SQL statements per page), we collect all room IDs from the page,
     * then run exactly two batch queries:</p>
     * <ol>
     *   <li>One JPQL JOIN for amenity names: {@code WHERE resource.id IN (:roomIds)}</li>
     *   <li>One native SQL for primary photo URLs: {@code WHERE resource_id IN (...) AND is_primary = true}</li>
     * </ol>
     * <p>Total SQL statements per page request: <strong>3</strong>
     * (1 search + 1 amenities + 1 photos), down from ~81.</p>
     */
    @Override
    public Page<AvailableRoomResponse> searchAvailableRooms(RoomSearchRequest request, Pageable pageable) {
        // Normalize amenity IDs to distinct values
        List<UUID> amenityIds = (request.amenities() != null)
                ? request.amenities().stream().distinct().collect(Collectors.toList())
                : null;

        // ── Step 1: Execute the main availability search query ─────────────────
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

        if (rawResults.isEmpty()) {
            return rawResults.map(row -> mapToResponse(row, request.checkIn(), request.checkOut(),
                    ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()),
                    Collections.emptyMap(), Collections.emptyMap()));
        }

        // ── Step 2: Collect all room IDs from this page ────────────────────────
        List<UUID> roomIds = rawResults.getContent().stream()
                .map(row -> (UUID) row[0])
                .collect(Collectors.toList());

        // ── Step 3: Batch-fetch amenity names (1 SQL, replaces N per-room SELECTs)
        Map<UUID, List<String>> amenitiesByRoom = buildAmenityMap(roomIds);

        // ── Step 4: Batch-fetch primary photo URLs (1 SQL, replaces N per-room SELECTs)
        Map<UUID, String> photoByRoom = buildPhotoMap(roomIds);

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());

        return rawResults.map(row -> mapToResponse(
                row, request.checkIn(), request.checkOut(), nights, amenitiesByRoom, photoByRoom));
    }

    @Override
    public boolean isRoomAvailableForDates(UUID resourceId, LocalDate checkIn, LocalDate checkOut) {
        return roomAvailabilityRepository.isRoomAvailable(resourceId, checkIn, checkOut);
    }

    /**
     * Returns details for a single room. Fetches amenities and photo with two targeted queries.
     */
    @Override
    public AvailableRoomResponse getRoomDetails(UUID resourceId, LocalDate checkIn, LocalDate checkOut) {
        Object[] row = roomAvailabilityRepository.findRoomById(resourceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found: " + resourceId));

        LocalDate in  = checkIn  != null ? checkIn  : LocalDate.now();
        LocalDate out = checkOut != null ? checkOut : in.plusDays(1);
        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));

        List<UUID> singleId = List.of(resourceId);
        Map<UUID, List<String>> amenitiesByRoom = buildAmenityMap(singleId);
        Map<UUID, String>       photoByRoom     = buildPhotoMap(singleId);

        return mapToResponse(row, in, out, nights, amenitiesByRoom, photoByRoom);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Runs one JPQL JOIN query to load all amenity names for the given room IDs
     * and groups them into a Map keyed by room UUID.
     *
     * <p>Replaces the N-per-room loop:
     * {@code resourceAmenityRepository.findByResourceId(roomId).stream()
     *         .map(link -> link.getAmenity().getName())...}</p>
     */
    private Map<UUID, List<String>> buildAmenityMap(List<UUID> roomIds) {
        if (roomIds.isEmpty()) return Collections.emptyMap();

        List<Object[]> rows = resourceAmenityRepository.findAmenityNamesByResourceIds(roomIds);

        Map<UUID, List<String>> map = new HashMap<>();
        for (Object[] row : rows) {
            UUID roomId      = (UUID)   row[0];
            String amenityName = (String) row[1];
            map.computeIfAbsent(roomId, k -> new ArrayList<>()).add(amenityName);
        }
        return map;
    }

    /**
     * Runs one native SQL query to load primary Cloudinary photo URLs for the
     * given room IDs and returns a Map keyed by room UUID.
     *
     * <p>Returns {@code null} for rooms with no uploaded primary photo — the
     * frontend {@code RoomCard} falls back to its stock-image map in that case.</p>
     */
    private Map<UUID, String> buildPhotoMap(List<UUID> roomIds) {
        if (roomIds.isEmpty()) return Collections.emptyMap();

        List<Object[]> rows = roomAvailabilityRepository.findPrimaryPhotosByRoomIds(roomIds);

        Map<UUID, String> map = new HashMap<>();
        for (Object[] row : rows) {
            // Native query returns PGobject/UUID depending on driver version — handle both
            UUID roomId;
            if (row[0] instanceof UUID u) {
                roomId = u;
            } else {
                roomId = UUID.fromString(row[0].toString());
            }
            map.put(roomId, (String) row[1]);
        }
        return map;
    }

    /**
     * Maps a raw SQL result row plus pre-fetched lookup maps to an {@link AvailableRoomResponse}.
     *
     * <p>This method is intentionally free of any repository calls — all data is passed in.
     * The N+1 pattern previously caused one repository call per row here.</p>
     */
    private AvailableRoomResponse mapToResponse(
            Object[] row,
            LocalDate checkIn,
            LocalDate checkOut,
            long nights,
            Map<UUID, List<String>> amenitiesByRoom,
            Map<UUID, String> photoByRoom) {

        UUID    roomId       = (UUID)   row[0];
        String  roomName     = (String) row[1];
        String  resourceType = (String) row[2];
        Integer capacity     = row[3] != null ? ((Number) row[3]).intValue() : null;

        // row[4] = specs (String / PGobject / Map depending on driver)
        Map<String, Object> specs   = null;
        String              bedType = null;
        if (row[4] instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) m;
            specs = casted;
            if (specs.get("bedType") != null) {
                bedType = specs.get("bedType").toString();
            }
        } else if (row[4] != null) {
            String specsStr = row[4].toString();
            if (specsStr.contains("\"bedType\"")) {
                int idx   = specsStr.indexOf("\"bedType\"");
                int start = specsStr.indexOf("\"", idx + 10) + 1;
                int end   = specsStr.indexOf("\"", start);
                if (start > 0 && end > start) {
                    bedType = specsStr.substring(start, end);
                }
            }
        }

        BigDecimal pricePerNight = row[5] != null ? new BigDecimal(row[5].toString()) : null;
        String     currency      = (String) row[6];
        UUID       hotelId       = (UUID)   row[7];
        String     hotelName     = (String) row[8];
        String     subdomain     = (String) row[9];

        // Use pre-fetched data — zero extra DB calls
        List<String> amenityNames  = amenitiesByRoom.getOrDefault(roomId, Collections.emptyList());
        String       primaryPhoto  = photoByRoom.get(roomId); // null = no photo uploaded

        BigDecimal totalPrice = (pricePerNight != null && nights > 0)
                ? pricePerNight.multiply(BigDecimal.valueOf(nights))
                : null;

        return new AvailableRoomResponse(
                new HotelInfo(hotelId, hotelName, subdomain),
                new RoomInfo(roomId, roomName, resourceType, capacity, bedType,
                        amenityNames, specs, primaryPhoto),
                new StayInfo(checkIn, checkOut, (int) nights),
                new PricingInfo(pricePerNight, totalPrice, currency)
        );
    }
}
