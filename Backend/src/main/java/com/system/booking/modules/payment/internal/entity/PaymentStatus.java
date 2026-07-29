package com.system.booking.modules.payment.internal.entity;

/**
 * All valid states a {@link Payment} record can be in.
 * Using an enum instead of raw strings eliminates the risk of typos
 * silently breaking the refund finder or history queries at runtime.
 */
public enum PaymentStatus {
    /** Payment record created but not yet processed. */
    PENDING,
    /** Wallet was successfully charged. */
    COMPLETED,
    /** Payment attempted but wallet balance was insufficient. */
    FAILED,
    /** A previously COMPLETED payment has been reversed. */
    REFUNDED
}
