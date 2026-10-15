package com.system.booking.modules.review.internal.service;

import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.review.internal.dto.ReviewResponseDto;
import com.system.booking.modules.review.internal.entity.Review;
import com.system.booking.modules.review.internal.exception.ReviewNotFoundException;
import com.system.booking.modules.review.internal.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// lets hotel staff (ADMIN / OWNER / SUPER_ADMIN) write or edit the reply on a review
@Service
@RequiredArgsConstructor
public class ReviewReplyService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final ReviewRepository reviewRepo;
    private final CustomerRepository customerRepo;

    // reply is a single editable field — every call just overwrites it and stamps who/when
    @Transactional
    public ReviewResponseDto replyToReview(UUID reviewId, UUID tenantId, UUID staffId, String role, String replyText) {
        Review review = findScopedReview(reviewId, tenantId, role);

        review.setReply(replyText);
        review.setRepliedBy(staffId);
        review.setRepliedByRole(role);
        review.setRepliedAt(LocalDateTime.now());

        return toDto(reviewRepo.save(review));
    }

    // SUPER_ADMIN can reach any tenant's review; ADMIN/OWNER are locked to their own tenant
    private Review findScopedReview(UUID reviewId, UUID tenantId, String role) {
        if (SUPER_ADMIN_ROLE.equals(role)) {
            return reviewRepo.findById(reviewId)
                    .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        }
        return reviewRepo.findByIdAndTenantId(reviewId, tenantId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    private ReviewResponseDto toDto(Review r) {
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
