package com.system.booking.modules.review.api;

import com.system.booking.modules.review.internal.dto.ReplyToReviewRequestDto;
import com.system.booking.modules.review.internal.dto.ReviewResponseDto;
import com.system.booking.modules.review.internal.service.ReviewReplyService;
import com.system.booking.modules.review.internal.service.ReviewService;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// review moderation dashboard for hotel staff.
// ADMIN/OWNER are always locked to their own tenant; SUPER_ADMIN has no tenant of
// their own and must pass ?tenantId= explicitly to view or reply to a specific hotel's reviews.
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SUPER_ADMIN')")
public class ReviewAdminController {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final ReviewService reviewService;
    private final ReviewReplyService reviewReplyService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponseDto>> listReviews(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID scopedTenantId = resolveTenantId(tenantId);
        Page<ReviewResponseDto> reviews = reviewService.getReviewsForTenant(scopedTenantId, PageRequest.of(page, size));
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponseDto> replyToReview(
            @PathVariable UUID reviewId,
            @RequestParam(required = false) UUID tenantId,
            @Valid @RequestBody ReplyToReviewRequestDto request) {

        HotelUserPrincipal principal = SecurityUtil.getCurrentHotelUserPrincipal();
        UUID scopedTenantId = resolveTenantId(tenantId);

        ReviewResponseDto review = reviewReplyService.replyToReview(
                reviewId, scopedTenantId, principal.id(), principal.role(), request.reply());
        return ResponseEntity.ok(review);
    }

    // never trust a client-supplied tenantId for ADMIN/OWNER — always pin to their own principal.
    // only SUPER_ADMIN (who has no tenantId of their own) is allowed to specify one.
    private UUID resolveTenantId(UUID requestedTenantId) {
        HotelUserPrincipal principal = SecurityUtil.getCurrentHotelUserPrincipal();

        if (SUPER_ADMIN_ROLE.equals(principal.role())) {
            if (requestedTenantId == null) {
                throw new IllegalArgumentException("tenantId is required for SUPER_ADMIN requests");
            }
            return requestedTenantId;
        }

        return principal.tenantId();
    }
}
