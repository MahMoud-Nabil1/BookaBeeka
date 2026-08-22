package com.system.booking.modules.availability.api;

import com.system.booking.modules.availability.internal.service.AvailabilityExceptionService;
import com.system.booking.modules.availability.internal.service.ScheduleRuleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", e.getMessage() != null ? e.getMessage() : "Entity not found"
        ));
    }
}
