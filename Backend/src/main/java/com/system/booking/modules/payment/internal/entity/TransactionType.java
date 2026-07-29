package com.system.booking.modules.payment.internal.entity;

/**
 * All valid types a {@link WalletTransaction} ledger entry can have.
 * Enum-typed to prevent typos in switch expressions and history queries.
 */
public enum TransactionType {
    /** Customer added funds to their global wallet. No booking attached. */
    DEPOSIT,
    /** Funds were deducted from the wallet to pay for a booking. */
    PAYMENT,
    /** A completed payment was reversed and funds returned to the wallet. */
    REFUND
}
