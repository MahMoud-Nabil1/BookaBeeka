package com.system.booking.modules.booking.api;

import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> confirmBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) UUID tenantId) {

        UUID effectiveTenantId = tenantId;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (effectiveTenantId == null && auth != null && auth.getPrincipal() instanceof HotelUserPrincipal hp) {
            effectiveTenantId = hp.tenantId();
        }

        bookingApi.confirmBooking(effectiveTenantId, bookingId);
        return ResponseEntity.ok(Map.of("message", "Booking confirmed", "bookingId", bookingId.toString()));
    }

    // cancel a booking — guests can cancel their own, owners/admins can cancel for their property
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'ADMIN')")
    public ResponseEntity<CancellationResultDto> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "Cancellation requested") String reason) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = null;
        if (authentication != null && authentication.getPrincipal() instanceof CustomerPrincipal cp) {
            actorId = cp.id();
            // Prevent guest from cancelling someone else's booking
            BookingDto booking = bookingApi.getBookingById(tenantId, bookingId);
            if (booking != null && !cp.id().equals(booking.customerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof HotelUserPrincipal hp) {
            actorId = hp.id();
            if (tenantId == null && hp.tenantId() != null) {
                tenantId = hp.tenantId();
            }
        }

        CancellationResultDto result = bookingApi.cancelBooking(tenantId, bookingId, reason, actorId);
        return ResponseEntity.ok(result);
    }

    // mark a booking as completed — admin/owner triggers this after checkout date
    // this is required before a customer can leave a review
    @PostMapping("/{bookingId}/complete")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> completeBooking(@PathVariable UUID bookingId) {
        bookingApi.completeBooking(bookingId);
        return ResponseEntity.ok(Map.of("message", "Booking completed", "bookingId", bookingId.toString()));
    }

    // get booking by ID
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDto> getBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) UUID tenantId) {

        BookingDto booking = bookingApi.getBookingById(tenantId, bookingId);
        return ResponseEntity.ok(booking);
    }

    // check booking status
    @GetMapping("/{bookingId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingStatusDto> getStatus(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) UUID tenantId) {

        BookingStatusDto status = bookingApi.getBookingStatus(tenantId, bookingId);
        return ResponseEntity.ok(status);
    }

    // list tenant bookings for staff/owner
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<BookingDto>> listTenantBookings(
            @RequestParam(required = false) UUID tenantId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID targetTenantId = tenantId;
        if (targetTenantId == null && authentication != null && authentication.getPrincipal() instanceof HotelUserPrincipal hp) {
            targetTenantId = hp.tenantId();
        }

        List<BookingDto> bookings = bookingApi.listBookingsForTenant(targetTenantId);
        return ResponseEntity.ok(bookings);
    }

    // list my bookings — for customers returns their own bookings; for hotel staff/owners returns tenant bookings
    @GetMapping("/mine")
    public ResponseEntity<List<BookingDto>> listMyBookings(
            @RequestParam(required = false) UUID tenantId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof HotelUserPrincipal hp) {
            UUID targetTenantId = (tenantId != null) ? tenantId : hp.tenantId();
            List<BookingDto> bookings = bookingApi.listBookingsForTenant(targetTenantId);
            return ResponseEntity.ok(bookings);
        } else if (principal instanceof CustomerPrincipal cp) {
            UUID customerId = cp.id();
            List<BookingDto> bookings = (tenantId != null)
                    ? bookingApi.listBookingsForCustomer(tenantId, customerId)
                    : bookingApi.listBookingsForCustomer(customerId);
            return ResponseEntity.ok(bookings);
        } else {
            try {
                UUID customerId = UUID.fromString(authentication.getName());
                List<BookingDto> bookings = (tenantId != null)
                        ? bookingApi.listBookingsForCustomer(tenantId, customerId)
                        : bookingApi.listBookingsForCustomer(customerId);
                return ResponseEntity.ok(bookings);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
    }
}
