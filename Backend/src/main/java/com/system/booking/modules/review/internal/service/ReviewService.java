package com.system.booking.modules.review.internal.service;

import com.system.booking.modules.booking.api.BookingDto;
import com.system.booking.modules.booking.api.BookingModuleApi;
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

import java.util.UUID;

// handles review creation (with eligibility checks against the booking module) and read-side queries
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String COMPLETED_STATUS = "COMPLETED";

    private final ReviewRepository reviewRepo;
    private final BookingModuleApi bookingApi;

    @Transactional
    public ReviewResponseDto createReview(CreateReviewRequestDto request, UUID customerId) {
        // only the booking's owner, and only once it's actually completed, can leave a review
        BookingDto booking = findEligibleBooking(request.tenantId(), request.bookingId(), customerId);

        if (reviewRepo.existsByBookingId(booking.bookingId())) {
            throw new DuplicateReviewException(booking.bookingId());
        }

        Review review = Review.builder()
                .tenantId(request.tenantId())
                .bookingId(booking.bookingId())
                .customerId(customerId)
                .serviceId(request.serviceId())
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

        if (!COMPLETED_STATUS.equals(booking.status())) {
            throw new BookingNotEligibleForReviewException(
                    "Only completed bookings can be reviewed (current status: " + booking.status() + ")");
        }

        return booking;
    }

    private ReviewResponseDto toDto(Review r) {
        return new ReviewResponseDto(
                r.getId(), r.getTenantId(), r.getBookingId(), r.getCustomerId(),
                r.getServiceId(), r.getStaffId(), r.getRating(), r.getComment(), r.getIsVerified(),
                r.getReply(), r.getRepliedBy(), r.getRepliedByRole(), r.getRepliedAt(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
