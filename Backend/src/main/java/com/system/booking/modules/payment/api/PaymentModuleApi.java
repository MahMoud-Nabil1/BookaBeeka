package com.system.booking.modules.payment.api;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public API for the Payment module.
 *
 * <p>This is the <strong>only</strong> entry point other modules should use to
 * interact with payment logic. No internal service, repository, or entity from
 * the {@code payment.internal} package should ever be imported by another module.</p>
 *
 * <p>Typical consumers:
 * <ul>
 *   <li><b>Booking module</b> — calls {@link #processPayment} during checkout
 *       and {@link #refundPayment} when a booking is cancelled.</li>
 *   <li><b>Customer module</b> — calls {@link #topUpWallet} when a customer
 *       adds funds via an external payment gateway.</li>
 * </ul>
 * </p>
 */
public interface PaymentModuleApi {

    /**
     * Credits the customer's global wallet and records a DEPOSIT ledger entry.
     * No tenantId needed — the wallet is a single global balance (like a prepaid card).
     *
     * @param customerId UUID of the customer whose wallet to credit
     * @param amount     positive amount to add
     * @return a lightweight view of the created ledger entry
     */
    WalletTransactionResponse topUpWallet(UUID customerId, BigDecimal amount);

    /**
     * Attempts a wallet payment for the given booking.
     * Throws {@link com.system.booking.modules.payment.internal.exception.InsufficientBalanceException}
     * if the customer's balance is insufficient (triggers full transaction rollback).
     *
     * @param bookingId        UUID of the booking being paid
     * @param customerId       UUID of the paying customer
     * @param tenantId         tenant context
     * @param paymentAmount    exact amount to deduct
     * @param idempotencyKey   optional client-supplied key preventing double-charges on retry
     * @param expectedCurrency optional ISO-4217 code the caller expects the wallet to be in
     * @return a lightweight view of the resulting Payment record (status COMPLETED)
     */
    PaymentResponse processPayment(UUID bookingId, UUID customerId, UUID tenantId, BigDecimal paymentAmount,
                                   String idempotencyKey, String expectedCurrency);

    /**
     * Refunds the payment for a cancelled booking back to the customer's wallet.
     * Only works if the booking has a COMPLETED payment; throws
     * {@link jakarta.persistence.EntityNotFoundException} otherwise.
     *
     * @param bookingId  UUID of the booking whose payment should be reversed
     * @param customerId UUID of the customer who receives the refund
     * @param tenantId   tenant context
     * @return a lightweight view of the updated Payment record (status REFUNDED)
     */
    PaymentResponse refundPayment(UUID bookingId, UUID customerId, UUID tenantId);

    /**
     * Current balance snapshot for a customer's global wallet.
     *
     * @param customerId UUID of the wallet owner
     * @return current balance, currency, and last-updated timestamp
     */
    CustomerBalanceResponse getCustomerBalance(UUID customerId);

    /**
     * Paginated wallet transaction history for a customer, newest-first.
     *
     * @param customerId UUID of the wallet owner
     * @param page       zero-based page index
     * @param size       page size
     */
    Page<CustomerTransactionDetail> getCustomerHistory(UUID customerId, int page, int size);

    /**
     * Paginated payment history for every booking belonging to a tenant, newest-first.
     *
     * @param tenantId UUID of the tenant
     * @param page     zero-based page index
     * @param size     page size
     */
    Page<TenantPaymentDetail> getTenantPaymentHistory(UUID tenantId, int page, int size);

    /**
     * Current accumulated revenue balance for a tenant.
     *
     * @param tenantId UUID of the tenant
     */
    TenantBalanceResponse getTenantBalance(UUID tenantId);

    /**
     * Lightweight summary of a booking's payment state for the admin dashboard.
     *
     * <p>Returns cleanly even when no Payment record exists yet (status = "UNPAID")
     * so the dashboard never needs to handle a 404 for unpaid bookings.</p>
     *
     * @param bookingId UUID of the booking to summarise
     * @return {@code {paymentId, bookingId, amountPaid, totalDue, status}}
     */
    PaymentSummaryResponse getPaymentSummaryForBooking(UUID bookingId);

    /**
     * Computes how much of a payment would be refunded under the tenant's
     * current cancellation policy.
     *
     * <p>Call this before showing a "Trigger Refund" button on the dashboard so
     * the admin sees the exact amount that will be credited back to the customer.
     * The calculation is time-sensitive: the applicable tier is determined by how
     * many hours remain until the booking's start time at the moment of the call.</p>
     *
     * @param paymentId UUID of the Payment record to evaluate
     * @return {@code {paymentId, bookingId, amountPaid, refundPercentage, refundableAmount, policyDescription}}
     */
    RefundEligibilityResponse getRefundEligibleAmount(UUID paymentId);
}