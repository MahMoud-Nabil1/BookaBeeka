package com.system.booking.modules.notification.api.event;

import com.system.booking.modules.notification.api.model.NotificationType;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published by various modules (Booking, Auth, Payment, Customer)
 * to trigger asynchronous notification dispatching.
 *
 * <p><b>Architecture & Decoupling:</b>
 * By using an event record instead of direct service invocation, producer modules
 * do not need to know anything about email templates, SMTP servers, or notification
 * tables. They simply publish a {@code NotificationEvent} via Spring's
 * {@link org.springframework.context.ApplicationEventPublisher}, ensuring strict
 * bounded-context separation.</p>
 *
 * <p><b>Tenant Context Propagation:</b>
 * Because event listeners run asynchronously in background thread pools where the
 * original request's {@code ThreadLocal} is not available, this event explicitly
 * encapsulates {@code tenantId}. This enables the listener to safely re-establish
 * the {@code TenantContext} in the worker thread.</p>
 *
 * @param tenantId       The tenant identifier for strict multi-tenant scoping.
 * @param customerId     The recipient customer identifier.
 * @param bookingId      Optional booking identifier associated with this notification (null for auth/general events).
 * @param type           The categorized notification type.
 * @param subject        The notification subject / email header line.
 * @param recipientEmail The target email address for delivery.
 * @param body           The notification message content (plain text or HTML).
 * @param metadata       Optional supplementary payload (e.g., OTP code, check-in date, room name).
 */
public record NotificationEvent(
        UUID tenantId,
        UUID customerId,
        UUID bookingId,
        NotificationType type,
        String subject,
        String recipientEmail,
        String body,
        Map<String, Object> metadata
) implements Serializable {

    /**
     * Compact constructor providing defensive copying for metadata and parameter validation.
     */
    public NotificationEvent {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null for tenant-scoped notifications");
        }
        // customerId may be null for platform-level notifications (e.g., SuperAdmin OTP/password reset)
        // where the sender is not a Customer entity
        if (type == null) {
            throw new IllegalArgumentException("NotificationType must not be null");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("recipientEmail must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        metadata = (metadata != null) ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
    }

    /**
     * Factory method for creating an event without optional bookingId and metadata.
     */
    public static NotificationEvent of(UUID tenantId, UUID customerId, NotificationType type,
                                       String subject, String recipientEmail, String body) {
        return new NotificationEvent(tenantId, customerId, null, type, subject, recipientEmail, body, Map.of());
    }

    /**
     * Factory method for creating an event with bookingId.
     */
    public static NotificationEvent of(UUID tenantId, UUID customerId, UUID bookingId, NotificationType type,
                                       String subject, String recipientEmail, String body) {
        return new NotificationEvent(tenantId, customerId, bookingId, type, subject, recipientEmail, body, Map.of());
    }

    /**
     * Factory method for creating an event with bookingId and metadata.
     */
    public static NotificationEvent of(UUID tenantId, UUID customerId, UUID bookingId, NotificationType type,
                                       String subject, String recipientEmail, String body, Map<String, Object> metadata) {
        return new NotificationEvent(tenantId, customerId, bookingId, type, subject, recipientEmail, body, metadata);
    }
}
