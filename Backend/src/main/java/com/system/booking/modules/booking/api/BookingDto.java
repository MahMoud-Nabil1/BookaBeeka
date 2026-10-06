package com.system.booking.modules.booking.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// full booking representation for reads — hotel-specialized
public record BookingDto(
        UUID bookingId,
        UUID tenantId,
        UUID customerId,   // also referred to as guestId in hotel context
        UUID roomId,       // hotel room being booked (maps to resource_id in DB)
        String roomName,
        String roomNumber,
        String roomTypeName,
        UUID serviceOfferingId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer numberOfRooms,
        String status,
        BigDecimal totalAmount,
        String currency,
        String specialRequests,
        String cancellationReason,
        Integer version,
        OffsetDateTime createdAt
) {
    // Backward-compatible constructor for existing tests and callers without room metadata
    public BookingDto(
            UUID bookingId, UUID tenantId, UUID customerId, UUID roomId,
            UUID serviceOfferingId, OffsetDateTime startTime, OffsetDateTime endTime,
            LocalDate checkInDate, LocalDate checkOutDate, Integer numberOfRooms,
            String status, BigDecimal totalAmount, String currency,
            String specialRequests, String cancellationReason, Integer version,
            OffsetDateTime createdAt
    ) {
        this(bookingId, tenantId, customerId, roomId, null, null, null,
             serviceOfferingId, startTime, endTime, checkInDate, checkOutDate,
             numberOfRooms, status, totalAmount, currency, specialRequests,
             cancellationReason, version, createdAt);
    }
}
