package com.system.booking.modules.payment.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for crediting a customer's wallet (Scenario 1 — Deposit).
 *
 * No tenantId needed — the wallet is a single global balance owned by the
 * customer, not tied to any specific tenant (like a prepaid card).
 *
 * @param customerId UUID of the customer whose wallet should be credited
 * @param amount     positive monetary amount to add to the wallet
 */
public record TopUpRequest(

        @NotNull(message = "customerId must not be null")
        UUID customerId,

        @NotNull(message = "amount must not be null")
        @Positive(message = "amount must be a positive value")
        BigDecimal amount
) {}

