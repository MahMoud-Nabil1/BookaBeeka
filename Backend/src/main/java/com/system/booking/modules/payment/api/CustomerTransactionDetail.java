package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full detail view of a single wallet ledger entry for the customer history endpoint.
 *
 * Contains every field available on the transaction plus a computed
 * {@code balanceBefore} so the customer can see exactly how their balance
 * moved with each operation.
 *
 * @param transactionId  unique ID of this ledger entry
 * @param transactionType DEPOSIT / PAYMENT / REFUND
 * @param amount         monetary amount of this transaction (always positive)
 * @param balanceBefore  wallet balance immediately BEFORE this transaction
 * @param balanceAfter   wallet balance immediately AFTER this transaction
 * @param currency       ISO-4217 currency code of the wallet
 * @param bookingId      linked booking UUID — null for DEPOSIT transactions
 * @param description    human-readable description of what happened
 * @param createdAt      exact timestamp of the transaction
 */
public record CustomerTransactionDetail(
        UUID          transactionId,
        String        transactionType,
        BigDecimal    amount,
        BigDecimal    balanceBefore,
        BigDecimal    balanceAfter,
        String        currency,
        UUID          bookingId,
        String        description,
        LocalDateTime createdAt
) {}
