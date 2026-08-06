package com.system.booking.modules.booking.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// REST endpoints for testing the booking engine manually
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingModuleApi bookingApi;

    // create a new booking — requires an idempotency key header
    @PostMapping
    public ResponseEntity<BookingConfirmationDto> createBooking(
            @RequestBody CreateBookingRequestDto request,
            @RequestHeader(value = "Idempotency-Key", defaultValue = "") String idempotencyKey) {

        // auto-generate a key if the client didn't send one
        if (idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        BookingConfirmationDto result = bookingApi.createBooking(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // simulate payment success
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Map<String, String>> confirmBooking(
            @PathVariable UUID bookingId,
            @RequestParam UUID tenantId) {

        bookingApi.confirmBooking(tenantId, bookingId);
        return ResponseEntity.ok(Map.of("message", "Booking confirmed", "bookingId", bookingId.toString()));
    }

    // cancel a booking
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<CancellationResultDto> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "Customer requested cancellation") String reason,
            @RequestParam(required = false) UUID actorId) {

        CancellationResultDto result = bookingApi.cancelBooking(tenantId, bookingId, reason, actorId);
        return ResponseEntity.ok(result);
    }

    // check booking status
    @GetMapping("/{bookingId}/status")
    public ResponseEntity<BookingStatusDto> getStatus(
            @PathVariable UUID bookingId,
            @RequestParam UUID tenantId) {

        BookingStatusDto status = bookingApi.getBookingStatus(tenantId, bookingId);
        return ResponseEntity.ok(status);
    }

    // list all bookings for a customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingDto>> listCustomerBookings(
            @PathVariable UUID customerId,
            @RequestParam UUID tenantId) {

        List<BookingDto> bookings = bookingApi.listBookingsForCustomer(tenantId, customerId);
        return ResponseEntity.ok(bookings);
    }
}
