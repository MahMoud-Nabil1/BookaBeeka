package com.system.booking.modules.notification.api.model;

/**
 * Lifecycle status of a notification record.
 *
 * <p><b>Lifecycle Flow:</b>
 * <pre>
 *   [PENDING] (Saved to DB before external network dispatch)
 *       │
 *       ├── Dispatch Succeeded ──► [SENT] (sentAt timestamp recorded)
 *       │
 *       └── Dispatch Failed    ──► [FAILED] (Error logged, ready for retry policy)
 * </pre>
 * </p>
 */
public enum NotificationStatus {

    /**
     * Initial state when a notification is created and queued for delivery.
     * Persisted immediately to ensure an audit record exists before calling external SMTP servers.
     */
    PENDING,

    /**
     * Successfully delivered to the mail transport / downstream provider.
     */
    SENT,

    /**
     * Failed delivery due to SMTP errors, network timeouts, or invalid recipient addresses.
     */
    FAILED
}
