package com.system.booking.modules.payment.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for initiating a wallet-based checkout (Scenario 2 — Payment).
 *
 * @param bookingId        UUID of the booking being paid for
 * @param customerId       UUID of the paying customer
 * @param tenantId         tenant context of the operation
 * @param paymentAmount    exact amount to deduct from the wallet
 * @param idempotencyKey   optional client-supplied key preventing double-charges on retry
 * @param expectedCurrency optional ISO-4217 code the caller expects the wallet to be in;
 *                         if supplied and it doesn't match the wallet's currency the
 *                         payment is rejected before any DB writes
 */
public record PaymentRequest(

        @NotNull(message = "bookingId must not be null")
        UUID bookingId,

        @NotNull(message = "customerId must not be null")
        UUID customerId,

        @NotNull(message = "tenantId must not be null")
        UUID tenantId,

        @NotNull(message = "paymentAmount must not be null")
        @Positive(message = "paymentAmount must be a positive value")
        BigDecimal paymentAmount,

        String idempotencyKey,

        String expectedCurrency
) {}