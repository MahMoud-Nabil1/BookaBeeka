package com.system.booking.modules.review.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// what ADMIN / OWNER / SUPER_ADMIN sends to reply to (or edit a reply on) a review
public record ReplyToReviewRequestDto(
        @NotBlank(message = "Reply text is required")
        @Size(max = 2000, message = "Reply must not exceed 2000 characters")
        String reply
) {}
