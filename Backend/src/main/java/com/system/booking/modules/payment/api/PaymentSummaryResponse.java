package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight summary of a booking's payment state, returned by
 * {@link PaymentModuleApi#getPaymentSummaryForBooking(UUID)}.
 *
 * <p>Designed for the admin dashboard's booking-detail view: one call gives
 * enough information to render a payment status badge and trigger a refund
 * action without additional round-trips.</p>
 *
 * @param paymentId   UUID of the Payment record (null if no payment exists yet)
 * @param bookingId   UUID of the booking this summary belongs to
 * @param amountPaid  actual amount charged (from the Payment record); zero if unpaid
 * @param totalDue    booking's total amount as stored on the Booking entity
 * @param status      current payment status — PENDING / COMPLETED / FAILED / REFUNDED,
 *                    or {@code "UNPAID"} if no Payment record exists for this booking
 */
public record PaymentSummaryResponse(
        UUID       paymentId,
        UUID       bookingId,
        BigDecimal amountPaid,
        BigDecimal totalDue,
        String     status
) {}
