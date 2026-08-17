package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Single row in the platform-wide money-flow audit feed.
 * Represents one {@code WalletTransaction} entry with resolved
 * FROM (customer) and TO (tenant) identities.
 */
public record PlatformTransactionResponse(

        UUID transactionId,

        /** DEPOSIT / PAYMENT / REFUND */
        String transactionType,

        BigDecimal amount,
        BigDecimal balanceAfter,

        // FROM — always present (every transaction has a customer wallet)
        UUID fromCustomerId,
        String fromCustomerEmail,
        String fromCustomerName,    // firstName + " " + lastName

        // TO — null for DEPOSIT (customer just topped up, no tenant involved)
        UUID toTenantId,

        // Context — null for DEPOSIT
        UUID bookingId,
        UUID paymentId,

        String description,
        LocalDateTime createdAt
) {}
