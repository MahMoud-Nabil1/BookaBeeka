package com.system.booking.modules.booking.internal.entity;

// all valid states a booking can be in
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    EXPIRED
}
