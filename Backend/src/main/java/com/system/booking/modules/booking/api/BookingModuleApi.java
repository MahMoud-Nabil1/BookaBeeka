package com.system.booking.modules.booking.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// public contract — other modules only interact with booking through this
public interface BookingModuleApi {

    BookingConfirmationDto createBooking(CreateBookingRequestDto request, String idempotencyKey);

    void confirmBooking(UUID tenantId, UUID bookingId);

    CancellationResultDto cancelBooking(UUID tenantId, UUID bookingId, String reason, UUID actorId);

    BookingDto rescheduleBooking(UUID tenantId, UUID bookingId, OffsetDateTime newStart, OffsetDateTime newEnd);

    BookingStatusDto getBookingStatus(UUID tenantId, UUID bookingId);

    List<BookingDto> listBookingsForCustomer(UUID tenantId, UUID customerId);
}
