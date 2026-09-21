package com.system.booking.modules.notification.api.model;

/**
 * Enumeration representing the supported categories of notifications within the BookaBeeka platform.
 *
 * <p><b>Design Rationale:</b>
 * Defining notification types as a strongly typed enum ensures compile-time validation across
 * producing modules (e.g., Booking, Auth, Customer) and allows the notification engine to
 * route, categorize, and apply type-specific templates or business rules consistently.</p>
 */
public enum NotificationType {

    /**
     * Sent to a customer upon successful payment confirmation and booking finalization.
     */
    BOOKING_CONFIRMED,

    /**
     * One-Time Password dispatch for two-factor authentication or phone/email verification.
     */
    OTP_REQUESTED,

    /**
     * Password reset link or token sent to a customer or staff user.
     */
    PASSWORD_RESET
}
