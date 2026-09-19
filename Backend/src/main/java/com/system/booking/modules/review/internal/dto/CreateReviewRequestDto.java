package com.system.booking.modules.review.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// what the customer sends to leave a review on a completed booking
public record CreateReviewRequestDto(
        @NotNull(message = "Tenant ID is required")
        UUID tenantId,

        @NotNull(message = "Booking ID is required")
        UUID bookingId,

        @NotNull(message = "Service ID is required")
        UUID serviceId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @Size(max = 2000, message = "Comment must not exceed 2000 characters")
        String comment
) {}
