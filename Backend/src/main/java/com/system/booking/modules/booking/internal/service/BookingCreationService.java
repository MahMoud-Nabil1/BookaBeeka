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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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

        // step 2: resolve dates
        java.time.LocalDate checkIn = request.checkInDate() != null
                ? request.checkInDate()
                : (request.start() != null ? request.start().toLocalDate() : java.time.LocalDate.now());
        java.time.LocalDate checkOut = request.checkOutDate() != null
                ? request.checkOutDate()
                : (request.end() != null ? request.end().toLocalDate() : checkIn.plusDays(1));

        // step 3: resolve price — from service offering if provided, else from room's pricePerNight × nights
        java.math.BigDecimal basePrice;
        var room = request.roomId() != null
                ? inventoryApi.getResourceByTenantAndId(request.tenantId(), request.roomId())
                : null;

        if (request.serviceOfferingId() != null) {
            ServiceOfferingResponse service = inventoryApi.getServiceOfferingByTenantAndId(
                    request.tenantId(), request.serviceOfferingId());
            basePrice = service.price();
        } else {
            // Hotel room booking: price = pricePerNight × nights
            java.math.BigDecimal price = room != null ? room.pricePerNight() : null;
            if (price == null && room != null && room.specs() != null && room.specs().get("pricePerNight") != null) {
                Object raw = room.specs().get("pricePerNight");
                if (raw instanceof java.math.BigDecimal bd) {
                    price = bd;
                } else if (raw instanceof Number n) {
                    price = java.math.BigDecimal.valueOf(n.doubleValue());
                } else {
                    try {
                        price = new java.math.BigDecimal(raw.toString().trim());
                    } catch (Exception ignored) {}
                }
            }
            if (price == null && room != null && room.roomTypeId() != null) {
                try {
                    var roomType = inventoryApi.getRoomTypeByTenantAndId(request.tenantId(), room.roomTypeId());
                    price = roomType.basePricePerNight();
                } catch (Exception ignored) {}
            }
            if (price == null) {
                price = java.math.BigDecimal.valueOf(150.00); // Standard default rate matching discovery search
            }
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights <= 0) nights = 1;
            basePrice = price.multiply(java.math.BigDecimal.valueOf(nights));
        }

        int rooms = request.numberOfRooms() != null && request.numberOfRooms() > 0
                ? request.numberOfRooms() : 1;

        // step 4: hotel date-range availability check
        if (request.roomId() != null) {
            boolean available = availabilityApi.isRoomAvailableForDates(
                    request.roomId(), checkIn, checkOut);
            if (!available) {
                throw new SlotUnavailableException("Room is not available for the requested dates");
            }
        }

        // step 5: persist the booking
        Booking booking = Booking.builder()
                .tenantId(request.tenantId())
                .customerId(customerId)
                .roomId(request.roomId())
                .serviceOfferingId(request.serviceOfferingId())
                .startTime(request.start() != null ? request.start() : checkIn.atTime(14, 0).atOffset(ZoneOffset.UTC))
                .endTime(request.end() != null ? request.end() : checkOut.atTime(11, 0).atOffset(ZoneOffset.UTC))
                .checkIn(checkIn)
                .checkOut(checkOut)
                .numberOfRooms(rooms)
                .specialRequests(request.specialRequests())
                .metadata(request.metadata() != null ? request.metadata() : Map.of())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(basePrice.multiply(java.math.BigDecimal.valueOf(rooms)))
                .currency(room != null && room.currency() != null ? room.currency() : "USD")
                .build();
        final Booking savedBooking = bookingRepo.save(booking);

        OffsetDateTime createdAtOdt = savedBooking.getCreatedAt() != null
                ? savedBooking.getCreatedAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now();

        // step 6: save idempotency response
        Map<String, Object> responseBody = Map.of(
                "bookingId", savedBooking.getId().toString(),
                "status",    savedBooking.getStatus().name(),
                "createdAt", createdAtOdt.toString()
        );
        idempotencyService.complete(request.tenantId(), idempotencyKey, 201, responseBody);

        // step 7: fire event (runs after commit)
        eventPublisher.publishEvent(new BookingCreatedEvent(
                savedBooking.getId(), savedBooking.getTenantId(), savedBooking.getCustomerId(),
                savedBooking.getRoomId(), savedBooking.getTotalAmount(), createdAtOdt));

        // step 8: dispatch notification event
        customerRepo.findById(customerId).ifPresent(customer -> {
            try {
                String customerName = ((customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                        (customer.getLastName() != null ? customer.getLastName() : "")).trim();
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
                                "customerName", customerName.isEmpty() ? "Guest" : customerName,
                                "checkIn", savedBooking.getCheckIn() != null ? savedBooking.getCheckIn().toString() : "",
                                "checkOut", savedBooking.getCheckOut() != null ? savedBooking.getCheckOut().toString() : "",
                                "totalAmount", savedBooking.getTotalAmount() != null ? savedBooking.getTotalAmount().toString() : "0.00",
                                "currency", savedBooking.getCurrency() != null ? savedBooking.getCurrency() : "USD"
                        )
                ));
            } catch (Exception e) {
                log.warn("Failed to dispatch booking creation notification: {}", e.getMessage());
            }
        });

        return new BookingConfirmationDto(
                savedBooking.getId(), savedBooking.getStatus().name(),
                null, createdAtOdt);
    }
}
