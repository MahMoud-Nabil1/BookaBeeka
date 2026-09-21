package com.system.booking.modules.review.api;

import com.system.booking.modules.review.internal.dto.CreateReviewRequestDto;
import com.system.booking.modules.review.internal.dto.ReviewResponseDto;
import com.system.booking.modules.review.internal.service.ReviewService;
import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // customer leaves a review for a booking they've already completed
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponseDto> createReview(@Valid @RequestBody CreateReviewRequestDto request) {
        CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();
        ReviewResponseDto review = reviewService.createReview(request, customer.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    // public listing of reviews for a given service — remember to permitAll() this path in SecurityConfig
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<Page<ReviewResponseDto>> getReviewsForService(
            @PathVariable UUID serviceId,
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReviewResponseDto> reviews = reviewService.getReviewsForService(tenantId, serviceId, PageRequest.of(page, size));
        return ResponseEntity.ok(reviews);
    }

    // the logged-in customer's own review history, across every tenant they've booked with
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ReviewResponseDto>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();
        Page<ReviewResponseDto> reviews = reviewService.getMyReviews(customer.id(), PageRequest.of(page, size));
        return ResponseEntity.ok(reviews);
    }
}
