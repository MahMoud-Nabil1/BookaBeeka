package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of a refund eligibility check, returned by
 * {@link PaymentModuleApi#getRefundEligibleAmount(UUID)}.
 *
 * <p>The dashboard should display these fields before presenting the
 * admin with a "Trigger Refund" button, so they know exactly how much
 * will be credited back to the customer's wallet.</p>
 *
 * @param paymentId         UUID of the payment being evaluated
 * @param bookingId         UUID of the linked booking
 * @param amountPaid        original amount charged
 * @param refundPercentage  applicable refund tier (0-100), or 0 if no policy applies
 * @param refundableAmount  exact amount that would be returned: {@code amountPaid * refundPercentage / 100}
 * @param policyDescription human-readable description of the matching policy tier,
 *                          e.g. "100% refund if cancelled 48+ hours before slot"
 */
public record RefundEligibilityResponse(
        UUID       paymentId,
        UUID       bookingId,
        BigDecimal amountPaid,
        int        refundPercentage,
        BigDecimal refundableAmount,
        String     policyDescription
) {}
