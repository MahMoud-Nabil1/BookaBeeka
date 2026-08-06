package com.system.booking.modules.booking.api;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.booking.internal.service.BookingCreationService;
import com.system.booking.modules.booking.internal.service.BookingLifecycleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

// glues the internal services to the public API contract
@Service
@RequiredArgsConstructor
public class BookingModuleApiImpl implements BookingModuleApi {

    private final BookingCreationService creationService;
    private final BookingLifecycleService lifecycleService;
    private final BookingRepository bookingRepo;

    @Override
    @Transactional
    public BookingConfirmationDto createBooking(CreateBookingRequestDto request, String idempotencyKey) {
        return creationService.createBooking(request, idempotencyKey);
    }

    @Override
    @Transactional
    public void confirmBooking(UUID tenantId, UUID bookingId) {
        lifecycleService.confirmBooking(tenantId, bookingId);
    }

    @Override
    @Transactional
    public CancellationResultDto cancelBooking(UUID tenantId, UUID bookingId, String reason, UUID actorId) {
        return lifecycleService.cancelBooking(tenantId, bookingId, reason, actorId);
    }

    @Override
    @Transactional
    public BookingDto rescheduleBooking(UUID tenantId, UUID bookingId, OffsetDateTime newStart, OffsetDateTime newEnd) {
        // cancel the old booking first
        lifecycleService.cancelForReschedule(tenantId, bookingId, null);

        // grab the old booking to copy its details
        Booking old = bookingRepo.findByTenantIdAndId(tenantId, bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        // create a fresh booking for the new time
        CreateBookingRequestDto newRequest = new CreateBookingRequestDto(
                tenantId, old.getCustomerId(), old.getResourceId(),
                newStart, newEnd, old.getTotalAmount(), old.getCurrency());

        String rescheduledKey = "reschedule-" + bookingId + "-" + System.currentTimeMillis();
        BookingConfirmationDto confirmation = creationService.createBooking(newRequest, rescheduledKey);

        // return the new booking as a full DTO
        Booking newBooking = bookingRepo.findById(confirmation.bookingId())
                .orElseThrow(() -> new EntityNotFoundException("New booking not found"));

        return toDto(newBooking);
    }

    @Override
    public BookingStatusDto getBookingStatus(UUID tenantId, UUID bookingId) {
        Booking booking = bookingRepo.findByTenantIdAndId(tenantId, bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        return new BookingStatusDto(booking.getId(), booking.getStatus().name(), booking.getVersion());
    }

    @Override
    public List<BookingDto> listBookingsForCustomer(UUID tenantId, UUID customerId) {
        return bookingRepo.findByTenantIdAndCustomerId(tenantId, customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // maps entity to DTO
    private BookingDto toDto(Booking b) {
        return new BookingDto(
                b.getId(), b.getTenantId(), b.getCustomerId(), b.getResourceId(),
                b.getStartTime(), b.getEndTime(), b.getStatus().name(),
                b.getTotalAmount(), b.getCurrency(), b.getCancellationReason(),
                b.getVersion(),
                b.getCreatedAt() != null ? b.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
    }
}
