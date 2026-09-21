package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.availability.api.AvailabilityModuleApi;
import com.system.booking.modules.booking.api.BookingConfirmationDto;
import com.system.booking.modules.booking.api.CreateBookingRequestDto;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.event.BookingCreatedEvent;
import com.system.booking.modules.booking.internal.exception.SlotUnavailableException;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.dto.response.ServiceOfferingResponse;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCreationService {

    private final BookingRepository bookingRepo;
    private final AvailabilityModuleApi availabilityApi;
    private final InventoryModuleApi inventoryApi;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerRepository customerRepo;

    @Transactional
    public BookingConfirmationDto createBooking(CreateBookingRequestDto request, UUID customerId, String idempotencyKey) {

        // step 1: idempotency replay check
        Optional<Map<String, Object>> cached = idempotencyService.begin(request.tenantId(), idempotencyKey);
        if (cached.isPresent()) {
            Map<String, Object> body = cached.get();
            return new BookingConfirmationDto(
                    UUID.fromString((String) body.get("bookingId")),
                    (String) body.get("status"),
                    body.get("lockId") != null ? UUID.fromString((String) body.get("lockId")) : null,
                    OffsetDateTime.parse((String) body.get("createdAt"))
            );
        }

        // step 2: resolve price — from service offering if provided, else from room's pricePerNight × nights
        java.math.BigDecimal basePrice;
        if (request.serviceOfferingId() != null) {
            ServiceOfferingResponse service = inventoryApi.getServiceOfferingByTenantAndId(
                    request.tenantId(), request.serviceOfferingId());
            basePrice = service.price();
        } else {
            // Hotel room booking: price = pricePerNight × nights
            var room = inventoryApi.getResourceByTenantAndId(request.tenantId(), request.roomId());
            if (room.pricePerNight() == null) {
                throw new IllegalArgumentException(
                    "Room has no pricePerNight set and no serviceOfferingId was provided. " +
                    "Either set a price on the room or pass a serviceOfferingId.");
            }
            long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
            if (nights <= 0) nights = 1;
            basePrice = room.pricePerNight().multiply(java.math.BigDecimal.valueOf(nights));
        }

        int rooms = request.numberOfRooms() != null && request.numberOfRooms() > 0
                ? request.numberOfRooms() : 1;

        // step 3: hotel date-range availability check
        // IMPORTANT: isRangeAvailable() is the LEGACY slot-based check (requires ScheduleRule records).
        // For hotel bookings we use isRoomAvailableForDates() which checks check_in/check_out
        // directly against the booking table — no schedule rules needed.
        boolean available = availabilityApi.isRoomAvailableForDates(
                request.roomId(), request.checkInDate(), request.checkOutDate());
        if (!available) {
            throw new SlotUnavailableException("Room is not available for the requested dates");
        }

        // step 4: persist the booking
        Booking booking = Booking.builder()
                .tenantId(request.tenantId())
                .customerId(customerId)
                .roomId(request.roomId())
                .serviceOfferingId(request.serviceOfferingId())
                .startTime(request.start())
                .endTime(request.end())
                .checkIn(request.checkInDate())
                .checkOut(request.checkOutDate())
                .numberOfRooms(request.numberOfRooms() != null ? request.numberOfRooms() : 1)
                .specialRequests(request.specialRequests())
                .metadata(request.metadata() != null ? request.metadata() : Map.of())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(basePrice.multiply(java.math.BigDecimal.valueOf(rooms)))
                .currency("USD")
                .build();
        final Booking savedBooking = bookingRepo.save(booking);

        OffsetDateTime createdAtOdt = savedBooking.getCreatedAt() != null
                ? savedBooking.getCreatedAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now();

        // step 5: save idempotency response
        Map<String, Object> responseBody = Map.of(
                "bookingId", savedBooking.getId().toString(),
                "status",    savedBooking.getStatus().name(),
                "createdAt", createdAtOdt.toString()
        );
        idempotencyService.complete(request.tenantId(), idempotencyKey, 201, responseBody);

        // step 6: fire event (runs after commit)
        eventPublisher.publishEvent(new BookingCreatedEvent(
                savedBooking.getId(), savedBooking.getTenantId(), savedBooking.getCustomerId(),
                savedBooking.getRoomId(), savedBooking.getTotalAmount(), createdAtOdt));

        // step 7: dispatch notification event
        customerRepo.findById(customerId).ifPresent(customer -> {
            eventPublisher.publishEvent(NotificationEvent.of(
                    savedBooking.getTenantId(),
                    savedBooking.getCustomerId(),
                    savedBooking.getId(),
                    NotificationType.BOOKING_CONFIRMED,
                    "Reservation Created - Hotel Booking #" + savedBooking.getId().toString().substring(0, 8),
                    customer.getEmail(),
                    "Your booking has been reserved successfully.",
                    Map.of(
                            "bookingId", savedBooking.getId().toString(),
                            "customerName", customer.getFirstName() + " " + customer.getLastName(),
                            "checkIn", savedBooking.getCheckIn() != null ? savedBooking.getCheckIn().toString() : "",
                            "checkOut", savedBooking.getCheckOut() != null ? savedBooking.getCheckOut().toString() : "",
                            "totalAmount", savedBooking.getTotalAmount() != null ? savedBooking.getTotalAmount().toString() : "0.00",
                            "currency", savedBooking.getCurrency()
                    )
            ));
        });

        return new BookingConfirmationDto(
                savedBooking.getId(), savedBooking.getStatus().name(),
                null, createdAtOdt);
    }
}
