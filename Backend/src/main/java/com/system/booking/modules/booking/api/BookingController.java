package com.system.booking.modules.booking.api;

import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingModuleApi bookingApi;

    // create a new booking — customerId comes from the logged-in customer's JWT
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingConfirmationDto> createBooking(
            @RequestBody CreateBookingRequestDto request,
            @RequestHeader(value = "Idempotency-Key", defaultValue = "") String idempotencyKey) {

        CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();

        if (idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        BookingConfirmationDto result = bookingApi.createBooking(request, customer.id(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // simulate payment success — keeping manual for now
    @PostMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> confirmBooking(
            @PathVariable UUID bookingId,
            @RequestParam UUID tenantId) {

        bookingApi.confirmBooking(tenantId, bookingId);
        return ResponseEntity.ok(Map.of("message", "Booking confirmed", "bookingId", bookingId.toString()));
    }

    // cancel a booking — only the owner can cancel their own booking
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CancellationResultDto> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "Customer requested cancellation") String reason) {

        CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();

        CancellationResultDto result = bookingApi.cancelBooking(tenantId, bookingId, reason, customer.id());
        return ResponseEntity.ok(result);
    }

    // check booking status
    @GetMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingStatusDto> getStatus(
            @PathVariable UUID bookingId,
            @RequestParam UUID tenantId) {

        BookingStatusDto status = bookingApi.getBookingStatus(tenantId, bookingId);
        return ResponseEntity.ok(status);
    }

    // list my bookings — customerId is extracted from JWT, not from the URL
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BookingDto>> listMyBookings(
            @RequestParam UUID tenantId) {

        CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();

        List<BookingDto> bookings = bookingApi.listBookingsForCustomer(tenantId, customer.id());
        return ResponseEntity.ok(bookings);
    }
}
