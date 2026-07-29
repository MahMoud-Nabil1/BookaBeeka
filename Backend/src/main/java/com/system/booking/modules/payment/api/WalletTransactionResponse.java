package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only view of a {@code WalletTransaction} ledger entry returned to the caller.
 *
 * @param id              unique ID of the transaction record
 * @param walletId        ID of the wallet this entry belongs to
 * @param bookingId       ID of the linked booking, or {@code null} for deposits
 * @param amount          monetary amount involved in this transaction
 * @param transactionType DEPOSIT / PAYMENT / REFUND
 * @param balanceAfter    wallet balance immediately after this transaction
 * @param description     human-readable description of the operation
 * @param createdAt       timestamp of when the transaction was recorded
 */
public record WalletTransactionResponse(
        UUID            id,
        UUID            walletId,
        UUID            bookingId,
        BigDecimal      amount,
        String          transactionType,
        BigDecimal      balanceAfter,
        String          description,
        LocalDateTime   createdAt
) {}
