package com.system.booking.modules.booking.internal.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event fired when a booking transitions to CANCELLED status.
 *
 * <p>Carries rich metadata allowing decoupled asynchronous listeners
 * (such as notification services) to construct full cancellation alerts
 * without tight coupling to the booking repository.</p>
 */
public record BookingCancelledEvent(
        UUID bookingId,
        UUID tenantId,
        UUID customerId,
        UUID roomId,
        UUID actorId,
        LocalDate checkIn,
        LocalDate checkOut,
        OffsetDateTime slotStart,
        OffsetDateTime slotEnd,
        BigDecimal totalAmount,
        String currency,
        BigDecimal refundAmount,
        Integer refundPercentage,
        String reason,
        OffsetDateTime cancelledAt
) {
    /**
     * Backward-compatible constructor for existing tests or callers.
     */
    public BookingCancelledEvent(UUID bookingId, UUID tenantId, BigDecimal refundAmount, String reason) {
        this(bookingId, tenantId, null, null, null, null, null, null, null, null, "USD",
                refundAmount, 0, reason, OffsetDateTime.now());
    }
}
