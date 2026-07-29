package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full detail view of a single payment record for the tenant history endpoint.
 *
 * Contains every piece of information a tenant needs to audit a payment:
 * who paid, for which booking, how much, what method, what the current
 * status is, and when each state change occurred.
 *
 * @param paymentId      unique ID of the payment record
 * @param bookingId      UUID of the booking this payment covers
 * @param status         current payment status: PENDING / COMPLETED / FAILED / REFUNDED
 * @param paymentMethod  always "WALLET" for wallet-based payments
 * @param amount         charged amount
 * @param currency       ISO-4217 currency code of the payment
 * @param createdAt      timestamp when the payment was first created (PENDING)
 * @param updatedAt      timestamp of the last status change (e.g. when it became COMPLETED)
 */
public record TenantPaymentDetail(
        UUID          paymentId,
        UUID          bookingId,
        String        status,
        String        paymentMethod,
        BigDecimal    amount,
        String        currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
