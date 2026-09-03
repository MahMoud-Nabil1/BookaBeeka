package com.system.booking.modules.availability.api;

import com.system.booking.modules.availability.api.dto.AvailableRoomResponse;
import com.system.booking.modules.availability.api.dto.CreateRoomBlockRequest;
import com.system.booking.modules.availability.api.dto.RoomBlockResponse;
import com.system.booking.modules.availability.api.dto.RoomSearchRequest;
import com.system.booking.modules.availability.api.dto.UpdateRoomBlockRequest;
import com.system.booking.modules.availability.internal.service.AvailabilityExceptionService;
import com.system.booking.modules.availability.internal.service.ScheduleRuleService;
import com.system.booking.modules.security.context.TenantContextHolder;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityModuleApi availabilityModuleApi;
    private final ScheduleRuleService scheduleRuleService;
    private final AvailabilityExceptionService exceptionService;

    // ── Hotel Room Search (public, no auth) ──────────────────────

    @GetMapping("/search")
    public ResponseEntity<Page<AvailableRoomResponse>> searchAvailableRooms(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String bedType,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<UUID> amenities,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("checkIn and checkOut are required");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("checkIn must be before checkOut");
        }

        RoomSearchRequest request = new RoomSearchRequest(
                hotelId, checkIn, checkOut, roomType, bedType,
                minCapacity, minPrice, maxPrice, amenities
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<AvailableRoomResponse> results = availabilityModuleApi.searchAvailableRooms(request, pageable);
        return ResponseEntity.ok(results);
    }

    // ── Room Block CRUD (admin, tenant from JWT) ─────────────────

    @PostMapping("/room-blocks")
    public ResponseEntity<RoomBlockResponse> createRoomBlock(
            @Valid @RequestBody CreateRoomBlockRequest req) {
        UUID tenantId = TenantContextHolder.getRequiredContext().tenantId();
        RoomBlockResponse block = exceptionService.createRoomBlock(tenantId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(block);
    }

    @GetMapping("/room-blocks")
    public ResponseEntity<List<RoomBlockResponse>> listRoomBlocks() {
        UUID tenantId = TenantContextHolder.getRequiredContext().tenantId();
        return ResponseEntity.ok(exceptionService.listRoomBlocks(tenantId));
    }

    @GetMapping("/room-blocks/{id}")
    public ResponseEntity<RoomBlockResponse> getRoomBlock(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredContext().tenantId();
        return ResponseEntity.ok(exceptionService.getRoomBlock(tenantId, id));
    }

    @PutMapping("/room-blocks/{id}")
    public ResponseEntity<RoomBlockResponse> updateRoomBlock(
            @PathVariable UUID id,
            @RequestBody UpdateRoomBlockRequest req) {
        UUID tenantId = TenantContextHolder.getRequiredContext().tenantId();
        return ResponseEntity.ok(exceptionService.updateRoomBlock(tenantId, id, req));
    }

    @DeleteMapping("/room-blocks/{id}")
    public ResponseEntity<Void> deleteRoomBlock(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredContext().tenantId();
        exceptionService.deleteRoomBlock(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Legacy slot-based endpoints (untouched) ──────────────────

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDto>> getSlots(
            @RequestParam UUID tenantId,
            @RequestParam UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            
        List<SlotDto> slots = availabilityModuleApi.getAvailableSlots(tenantId, resourceId, date);
        return ResponseEntity.ok(slots);
    }



    @PostMapping("/schedule-rules")
    public ResponseEntity<Map<String, Object>> createScheduleRule(
            @RequestParam UUID tenantId,
            @RequestParam UUID resourceId,
            @RequestBody ScheduleRuleDto rule) {
        availabilityModuleApi.defineScheduleRule(tenantId, resourceId, rule);
        // fetch the latest rules for this resource to return the newly created one
        var rules = scheduleRuleService.listRulesForResource(resourceId);
        var created = rules.get(rules.size() - 1);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", created.getId(),
                "tenantId", created.getTenantId(),
                "resourceId", created.getResource().getId(),
                "dayOfWeek", created.getDayOfWeek(),
                "startTime", created.getStartTime(),
                "endTime", created.getEndTime()
        ));
    }

    @GetMapping("/schedule-rules")
    public ResponseEntity<List<Map<String, Object>>> listScheduleRules(@RequestParam UUID resourceId) {
        var rules = scheduleRuleService.listRulesForResource(resourceId);
        var response = rules.stream().map(rule -> Map.<String, Object>of(
                "id", rule.getId(),
                "tenantId", rule.getTenantId(),
                "resourceId", rule.getResource().getId(),
                "dayOfWeek", rule.getDayOfWeek(),
                "startTime", rule.getStartTime(),
                "endTime", rule.getEndTime()
        )).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/schedule-rules/{ruleId}")
    public ResponseEntity<Void> updateScheduleRule(
            @PathVariable UUID ruleId,
            @RequestParam UUID tenantId,
            @RequestBody ScheduleRuleDto rule) {
        availabilityModuleApi.updateScheduleRule(tenantId, ruleId, rule);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/schedule-rules/{ruleId}")
    public ResponseEntity<Void> deleteScheduleRule(
            @PathVariable UUID ruleId,
            @RequestParam UUID tenantId) {
        availabilityModuleApi.deleteScheduleRule(tenantId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exceptions")
    public ResponseEntity<Map<String, Object>> createException(
            @RequestParam UUID tenantId,
            @RequestParam UUID resourceId,
            @RequestBody ExceptionDto exception) {
        availabilityModuleApi.addAvailabilityException(tenantId, resourceId, exception);
        var exceptions = exceptionService.listExceptions(resourceId);
        var created = exceptions.get(exceptions.size() - 1);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", created.getId(),
                "tenantId", created.getTenantId(),
                "resourceId", created.getResource().getId(),
                "exceptionDate", created.getExceptionDate(),
                "isAvailable", created.getIsAvailable(),
                "reason", created.getReason() != null ? created.getReason() : ""
        ));
    }

    @GetMapping("/exceptions")
    public ResponseEntity<List<Map<String, Object>>> listExceptions(@RequestParam UUID resourceId) {
        var exceptions = exceptionService.listExceptions(resourceId);
        var response = exceptions.stream().map(exception -> Map.<String, Object>of(
                "id", exception.getId(),
                "tenantId", exception.getTenantId(),
                "resourceId", exception.getResource().getId(),
                "exceptionDate", exception.getExceptionDate(),
                "isAvailable", exception.getIsAvailable(),
                "startTime", exception.getStartTime(),
                "endTime", exception.getEndTime(),
                "reason", exception.getReason()
        )).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/exceptions/{exceptionId}")
    public ResponseEntity<Void> updateException(
            @PathVariable UUID exceptionId,
            @RequestParam UUID tenantId,
            @RequestBody ExceptionDto exception) {
        availabilityModuleApi.updateAvailabilityException(tenantId, exceptionId, exception);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/exceptions/{exceptionId}")
    public ResponseEntity<Void> deleteException(
            @PathVariable UUID exceptionId,
            @RequestParam UUID tenantId) {
        availabilityModuleApi.deleteAvailabilityException(tenantId, exceptionId);
        return ResponseEntity.noContent().build();
    }

    // ── Exception Handlers ───────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", e.getMessage() != null ? e.getMessage() : "Entity not found"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", e.getMessage() != null ? e.getMessage() : "Invalid request"
        ));
    }
}

