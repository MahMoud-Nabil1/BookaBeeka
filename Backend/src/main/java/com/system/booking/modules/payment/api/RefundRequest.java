package com.system.booking.modules.payment.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for cancelling a booking and refunding the customer's wallet
 * (Scenario 3 — Refund).
 *
 * @param bookingId  UUID of the booking whose payment should be reversed
 * @param customerId UUID of the customer who receives the refund
 * @param tenantId   tenant context of the operation
 */
public record RefundRequest(

        @NotNull(message = "bookingId must not be null")
        UUID bookingId,

        @NotNull(message = "customerId must not be null")
        UUID customerId,

        @NotNull(message = "tenantId must not be null")
        UUID tenantId
) {}

