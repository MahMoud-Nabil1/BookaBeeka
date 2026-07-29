package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only view of a {@code Payment} record returned to the caller.
 * Avoids exposing JPA entities across module boundaries.
 *
 * @param id            unique ID of the payment record
 * @param bookingId     ID of the linked booking
 * @param amount        charged amount
 * @param currency      ISO-4217 currency code (e.g. "EGP", "USD", "EUR")
 * @param status        current payment status (PENDING / COMPLETED / FAILED / REFUNDED)
 * @param paymentMethod always "WALLET" for this service
 * @param createdAt     timestamp of when the payment was first created
 */
public record PaymentResponse(
        UUID            id,
        UUID            bookingId,
        BigDecimal      amount,
        String          currency,
        String          status,
        String          paymentMethod,
        LocalDateTime   createdAt
) {}
