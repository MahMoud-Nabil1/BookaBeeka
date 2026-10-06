package com.system.booking.modules.review.internal.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// full review representation for reads — includes customer display name and room linkage
public record ReviewResponseDto(
        UUID id,
        UUID tenantId,
        UUID bookingId,
        UUID customerId,
        String customerFirstName,
        String customerLastName,
        UUID serviceId,
        UUID roomId,
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
) {
    // Backward-compatible constructor for existing tests and callers without customer name and room linkage
    public ReviewResponseDto(
            UUID id, UUID tenantId, UUID bookingId, UUID customerId,
            UUID serviceId, UUID staffId, Integer rating, String comment,
            Boolean isVerified, String reply, UUID repliedBy,
            String repliedByRole, LocalDateTime repliedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this(id, tenantId, bookingId, customerId, null, null, serviceId, null, staffId, rating, comment, isVerified, reply, repliedBy, repliedByRole, repliedAt, createdAt, updatedAt);
    }
}
