package com.system.booking.modules.notification.internal.exception;

import java.util.UUID;

/**
 * Thrown when a requested notification cannot be found for the given ID and tenant scope.
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID notificationId) {
        super("Notification not found with ID: " + notificationId);
    }

    public NotificationNotFoundException(UUID tenantId, UUID notificationId) {
        super(String.format("Notification %s not found under tenant %s", notificationId, tenantId));
    }
}
