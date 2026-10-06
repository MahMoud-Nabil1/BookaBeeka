package com.system.booking.modules.review.internal.service;

import com.system.booking.modules.booking.api.BookingDto;
import com.system.booking.modules.booking.api.BookingModuleApi;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.review.internal.dto.CreateReviewRequestDto;
import com.system.booking.modules.review.internal.dto.ReviewResponseDto;
import com.system.booking.modules.review.internal.entity.Review;
import com.system.booking.modules.review.internal.exception.BookingNotEligibleForReviewException;
import com.system.booking.modules.review.internal.exception.DuplicateReviewException;
import com.system.booking.modules.review.internal.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

// handles review creation (with eligibility checks against the booking module) and read-side queries
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String COMPLETED_STATUS = "COMPLETED";
    // A CONFIRMED booking whose end time has already passed is also reviewable —
    // the stay is over even if admin hasn't explicitly moved it to COMPLETED yet.
    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private final ReviewRepository reviewRepo;
    private final BookingModuleApi bookingApi;
    private final InventoryModuleApi inventoryApi;
    private final CustomerRepository customerRepo;

    @Transactional
    public ReviewResponseDto createReview(CreateReviewRequestDto request, UUID customerId) {
        // only the booking's owner, and only once it's actually completed, can leave a review
        BookingDto booking = findEligibleBooking(request.tenantId(), request.bookingId(), customerId);

        if (reviewRepo.existsByBookingId(booking.bookingId())) {
            throw new DuplicateReviewException(booking.bookingId());
        }

        // serviceId must point to a row in the `service` table (service_offering).
        // Prefer the explicit serviceOfferingId on the booking, but only if it still exists —
        // it may be a stale UUID from a deleted service. Fall back to looking up a linked
        // service offering via the room (ResourceServiceLink) if the direct ID is missing or gone.
        UUID serviceId = booking.serviceOfferingId();
        if (serviceId != null && !inventoryApi.serviceOfferingExists(serviceId)) {
            // stale serviceOfferingId — treat it as absent and use the room fallback
            serviceId = null;
        }
        if (serviceId == null && booking.roomId() != null) {
            serviceId = inventoryApi.getFirstServiceOfferingIdForRoom(booking.tenantId(), booking.roomId());
        }
        // serviceId is best-effort: null is acceptable for room-only bookings with no linked service offering.
        // The column is nullable in the DB, and reviews must not be blocked by missing service links.

        Review review = Review.builder()
                .tenantId(request.tenantId())
                .bookingId(booking.bookingId())
                .customerId(customerId)
                .serviceId(serviceId)
                .roomId(booking.roomId())   // always capture the room for direct room-based queries
                .rating(request.rating())
                .comment(request.comment())
                .isVerified(true) // backed by a real, completed booking
                .build();

        return toDto(reviewRepo.save(review));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsForService(UUID tenantId, UUID serviceId, Pageable pageable) {
        return reviewRepo.findByTenantIdAndServiceId(tenantId, serviceId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsForRoom(UUID tenantId, UUID roomId, Pageable pageable) {
        return reviewRepo.findByTenantIdAndRoomId(tenantId, roomId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsForTenant(UUID tenantId, Pageable pageable) {
        return reviewRepo.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getMyReviews(UUID customerId, Pageable pageable) {
        return reviewRepo.findByCustomerId(customerId, pageable).map(this::toDto);
    }

    // fetches a single booking directly (O(1)) instead of loading the whole history and filtering.
    // then explicitly verifies ownership and completion status.
    private BookingDto findEligibleBooking(UUID tenantId, UUID bookingId, UUID customerId) {
        BookingDto booking;
        try {
            booking = bookingApi.getBookingById(tenantId, bookingId);
        } catch (EntityNotFoundException e) {
            throw new BookingNotEligibleForReviewException(
                    "Booking not found or does not belong to you: " + bookingId);
        }

        // guard against a customer trying to review someone else's booking
        if (!customerId.equals(booking.customerId())) {
            throw new BookingNotEligibleForReviewException(
                    "Booking not found or does not belong to you: " + bookingId);
        }

        boolean isCompleted = COMPLETED_STATUS.equals(booking.status());
        // A CONFIRMED booking is also reviewable once the guest has checked in (startTime passed or checkIn date reached) —
        // covers mid-stay and post-checkout cases where admin hasn't moved it to COMPLETED yet.
        boolean stayStarted = (booking.startTime() != null && booking.startTime().isBefore(OffsetDateTime.now()))
                || (booking.checkInDate() != null && !booking.checkInDate().isAfter(java.time.LocalDate.now()));
        boolean isConfirmedAndStarted = CONFIRMED_STATUS.equals(booking.status()) && stayStarted;

        if (!isCompleted && !isConfirmedAndStarted) {
            throw new BookingNotEligibleForReviewException(
                    "Only completed bookings (or confirmed bookings whose stay has started) can be reviewed " +
                    "(current status: " + booking.status() + ")");
        }

        return booking;
    }

    private ReviewResponseDto toDto(Review r) {
        // look up customer name — best-effort: fall back to empty string if customer row is missing
        String firstName = "";
        String lastName = "";
        if (r.getCustomerId() != null) {
            Customer customer = customerRepo.findById(r.getCustomerId()).orElse(null);
            if (customer != null) {
                firstName = customer.getFirstName() != null ? customer.getFirstName() : "";
                lastName  = customer.getLastName()  != null ? customer.getLastName()  : "";
            }
        }

        return new ReviewResponseDto(
                r.getId(), r.getTenantId(), r.getBookingId(), r.getCustomerId(),
                firstName, lastName,
                r.getServiceId(), r.getRoomId(), r.getStaffId(),
                r.getRating(), r.getComment(), r.getIsVerified(),
                r.getReply(), r.getRepliedBy(), r.getRepliedByRole(), r.getRepliedAt(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
