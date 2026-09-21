package com.system.booking.modules.availability.api;

import com.system.booking.modules.availability.api.dto.AvailableRoomResponse;
import com.system.booking.modules.availability.api.dto.RoomSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AvailabilityModuleApi {

    // ── Legacy slot-based API (untouched) ────────────────────────
    List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, LocalDate date);
    List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, UUID serviceOfferingId, LocalDate date);
    SlotLockDto lockSlot(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end, UUID userId);
    void releaseLock(UUID tenantId, UUID lockId);
    void consumeLock(UUID tenantId, UUID lockId, UUID bookingId);
    boolean isRangeAvailable(UUID tenantId, UUID resourceId, OffsetDateTime start, OffsetDateTime end);
    void defineScheduleRule(UUID tenantId, UUID resourceId, ScheduleRuleDto rule);
    void updateScheduleRule(UUID tenantId, UUID ruleId, ScheduleRuleDto rule);
    void deleteScheduleRule(UUID tenantId, UUID ruleId);
    void addAvailabilityException(UUID tenantId, UUID resourceId, ExceptionDto exception);
    void updateAvailabilityException(UUID tenantId, UUID exceptionId, ExceptionDto exception);
    void deleteAvailabilityException(UUID tenantId, UUID exceptionId);

    // ── Hotel date-range availability API ────────────────────────
    Page<AvailableRoomResponse> searchAvailableRooms(RoomSearchRequest request, Pageable pageable);
    boolean isRoomAvailableForDates(UUID resourceId, LocalDate checkIn, LocalDate checkOut);
}
