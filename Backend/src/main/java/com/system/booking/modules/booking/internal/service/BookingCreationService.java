package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.availability.api.AvailabilityModuleApi;
import com.system.booking.modules.availability.api.SlotLockDto;
import com.system.booking.modules.booking.api.BookingConfirmationDto;
import com.system.booking.modules.booking.api.CreateBookingRequestDto;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.event.BookingCreatedEvent;
import com.system.booking.modules.booking.internal.exception.SlotUnavailableException;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.dto.ServiceOfferingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// handles the critical booking creation path
@Service
@RequiredArgsConstructor
public class BookingCreationService {

    private final BookingRepository bookingRepo;
    private final AvailabilityModuleApi availabilityApi;
    private final InventoryModuleApi inventoryApi;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingConfirmationDto createBooking(CreateBookingRequestDto request, UUID customerId, String idempotencyKey) {
        // step 1: check if this is a replay
        Optional<Map<String, Object>> cached = idempotencyService.begin(request.tenantId(), idempotencyKey);
        if (cached.isPresent()) {
            Map<String, Object> body = cached.get();
            return new BookingConfirmationDto(
                    UUID.fromString((String) body.get("bookingId")),
                    (String) body.get("status"),
                    UUID.fromString((String) body.get("lockId")),
                    OffsetDateTime.parse((String) body.get("createdAt"))
            );
        }

        // step 2: look up the service offering to get price and validate it exists
        ServiceOfferingResponse service = inventoryApi.getServiceOfferingByTenantAndId(
                request.tenantId(), request.serviceOfferingId());

        // step 3: check if the slot is actually free
        boolean available = availabilityApi.isRangeAvailable(
                request.tenantId(), request.resourceId(), request.start(), request.end());
        if (!available) {
            throw new SlotUnavailableException("Slot is already booked or locked");
        }

        // step 4: grab a temporary lock on the slot
        SlotLockDto lock;
        try {
            lock = availabilityApi.lockSlot(
                    request.tenantId(), request.resourceId(),
                    request.start(), request.end(), customerId);
        } catch (Exception e) {
            throw new SlotUnavailableException("Failed to lock slot: " + e.getMessage());
        }

        // step 5: persist the booking — price comes from the service offering
        Booking booking = Booking.builder()
                .tenantId(request.tenantId())
                .customerId(customerId)
                .resourceId(request.resourceId())
                .serviceOfferingId(request.serviceOfferingId())
                .lockId(lock.lockId())
                .startTime(request.start())
                .endTime(request.end())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(service.price())
                .currency("USD")
                .build();
        booking = bookingRepo.save(booking);

        // step 6: consume the lock so it can't be reused
        availabilityApi.consumeLock(request.tenantId(), lock.lockId(), booking.getId());

        // safely convert LocalDateTime to OffsetDateTime for the response
        OffsetDateTime createdAtOdt = booking.getCreatedAt() != null
                ? booking.getCreatedAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now();

        // step 7: save the idempotency response for future replays
        Map<String, Object> responseBody = Map.of(
                "bookingId", booking.getId().toString(),
                "status", booking.getStatus().name(),
                "lockId", lock.lockId().toString(),
                "createdAt", createdAtOdt.toString()
        );
        idempotencyService.complete(request.tenantId(), idempotencyKey, 201, responseBody);

        // step 8: fire the event (listeners run after commit)
        eventPublisher.publishEvent(new BookingCreatedEvent(
                booking.getId(), booking.getTenantId(), booking.getCustomerId(),
                booking.getResourceId(), booking.getTotalAmount(), createdAtOdt));

        return new BookingConfirmationDto(
                booking.getId(), booking.getStatus().name(),
                lock.lockId(), createdAtOdt);
    }
}
