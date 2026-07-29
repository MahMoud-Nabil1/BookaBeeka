package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Current balance snapshot for a customer's global wallet.
 *
 * @param walletId  unique ID of the wallet record
 * @param customerId UUID of the wallet owner
 * @param balance   current available balance
 * @param currency  ISO-4217 currency code
 * @param updatedAt timestamp of last balance change
 */
public record CustomerBalanceResponse(
        UUID          walletId,
        UUID          customerId,
        BigDecimal    balance,
        String        currency,
        LocalDateTime updatedAt
) {}
