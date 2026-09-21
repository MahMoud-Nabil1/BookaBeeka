package com.system.booking.modules.review.internal.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// full review representation for reads — includes the reply fields when present
public record ReviewResponseDto(
        UUID id,
        UUID tenantId,
        UUID bookingId,
        UUID customerId,
        UUID serviceId,
        UUID staffId,
        Integer rating,
        String comment,
        Boolean isVerified,
        String reply,
        UUID repliedBy,
        String repliedByRole,
        LocalDateTime repliedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
