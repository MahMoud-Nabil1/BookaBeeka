package com.system.booking.modules.notification.api.dto;

import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.entity.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a notification returned by API endpoints.
 *
 * <p><b>Decoupling Guarantee:</b>
 * Prevents direct exposure of the internal JPA entity, avoid lazy loading issues
 * outside transaction boundaries, and flattens relations (e.g., customerId, bookingId).</p>
 */
public record NotificationResponseDto(
        UUID id,
        UUID tenantId,
        UUID customerId,
        UUID bookingId,
        NotificationType type,
        String subject,
        String body,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        Integer retryCount,
        String failureReason
) {
    /**
     * Backward-compatible constructor without retryCount and failureReason.
     */
    public NotificationResponseDto(
            UUID id, UUID tenantId, UUID customerId, UUID bookingId,
            NotificationType type, String subject, String body,
            NotificationStatus status, LocalDateTime sentAt, LocalDateTime createdAt
    ) {
        this(id, tenantId, customerId, bookingId, type, subject, body, status, sentAt, createdAt, 0, null);
    }

    /**
     * Converts a JPA {@link Notification} entity into a {@link NotificationResponseDto}.
     * Safely traverses lazy relationships to extract primary key identifiers.
     *
     * @param notification The source notification entity.
     * @return Transformed response DTO.
     */
    public static NotificationResponseDto fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }

        UUID customerId = (notification.getCustomer() != null) ? notification.getCustomer().getId() : null;
        UUID bookingId = (notification.getBooking() != null) ? notification.getBooking().getId() : null;

        return new NotificationResponseDto(
                notification.getId(),
                notification.getTenantId(),
                customerId,
                bookingId,
                notification.getType(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getSentAt(),
                notification.getCreatedAt(),
                notification.getRetryCount(),
                notification.getFailureReason()
        );
    }
}
