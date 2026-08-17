package com.system.booking.modules.payment.internal.service;

import com.system.booking.modules.booking.internal.entity.CancellationPolicy;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.booking.internal.repository.CancellationPolicyRepository;
import com.system.booking.modules.payment.api.CustomerBalanceResponse;
import com.system.booking.modules.payment.api.CustomerTransactionDetail;
import com.system.booking.modules.payment.api.PaymentSummaryResponse;
import com.system.booking.modules.payment.api.RefundEligibilityResponse;
import com.system.booking.modules.payment.api.TenantBalanceResponse;
import com.system.booking.modules.payment.api.TenantPaymentDetail;
import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.TenantWallet;
import com.system.booking.modules.payment.internal.entity.TransactionType;
import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import com.system.booking.modules.payment.internal.repository.CustomerWalletRepository;
import com.system.booking.modules.payment.internal.repository.PaymentRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.payment.internal.repository.WalletTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only query service for payment history and balance inquiries.
 *
 * All methods are {@code @Transactional(readOnly = true)}, which:
 * <ul>
 *   <li>Tells Hibernate to skip dirty-checking on entity snapshots (performance gain)</li>
 *   <li>Routes to a read-replica in multi-datasource setups</li>
 * </ul>
 *
 * Separated from {@link WalletPaymentService} by design (SRP) — this service
 * never mutates state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentRepository           paymentRepository;
    private final TenantWalletRepository      tenantWalletRepository;
    private final CustomerWalletRepository    customerWalletRepository;
    private final BookingRepository           bookingRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;

    // -------------------------------------------------------------------------
    // Customer history (paginated)
    // -------------------------------------------------------------------------

    /**
     * Paginated wallet transaction history for a customer.
     * Each entry includes computed balanceBefore so the customer can trace
     * their balance movement entry by entry.
     */
    @Transactional(readOnly = true)
    public Page<CustomerTransactionDetail> getCustomerHistory(UUID customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return walletTransactionRepository
                .findByWalletCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toCustomerTransactionDetail);
    }

    // -------------------------------------------------------------------------
    // Customer balance
    // -------------------------------------------------------------------------

    /**
     * Current balance snapshot for a customer's global wallet.
     */
    @Transactional(readOnly = true)
    public CustomerBalanceResponse getCustomerBalance(UUID customerId) {
        return customerWalletRepository.findByCustomerId(customerId)
                .map(w -> new CustomerBalanceResponse(
                        w.getId(),
                        customerId,
                        w.getBalance(),
                        w.getCurrency(),
                        w.getUpdatedAt()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "No wallet found for customer: " + customerId));
    }

    // -------------------------------------------------------------------------
    // Tenant history (paginated)
    // -------------------------------------------------------------------------

    /**
     * Paginated payment history for all of a tenant's bookings, all statuses.
     */
    @Transactional(readOnly = true)
    public Page<TenantPaymentDetail> getTenantPaymentHistory(UUID tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return paymentRepository
                .findByBookingTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toTenantPaymentDetail);
    }

    // -------------------------------------------------------------------------
    // Tenant balance
    // -------------------------------------------------------------------------

    /**
     * Current revenue balance for a tenant.
     */
    @Transactional(readOnly = true)
    public TenantBalanceResponse getTenantBalance(UUID tenantId) {
        TenantWallet wallet = tenantWalletRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No revenue wallet for tenant: " + tenantId +
                        ". Auto-created on first completed payment."));

        return new TenantBalanceResponse(
                wallet.getId(),
                wallet.getTenant().getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // Payment summary for booking (dashboard: booking-detail view)
    // -------------------------------------------------------------------------

    /**
     * Returns the payment state of a booking in a single call.
     *
     * <p>If no Payment record exists yet the response still returns cleanly:
     * {@code paymentId} is null, {@code amountPaid} is zero, and
     * {@code status} is {@code "UNPAID"}.</p>
     *
     * @param bookingId UUID of the booking to summarise
     * @return amountPaid / totalDue / status
     */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummaryForBooking(UUID bookingId) {
        // totalDue comes from the Booking record itself
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Booking not found: " + bookingId));

        // Most recent payment for this booking (COMPLETED/REFUNDED takes priority over PENDING)
        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);

        if (payments.isEmpty()) {
            return new PaymentSummaryResponse(
                    null,
                    bookingId,
                    BigDecimal.ZERO,
                    booking.getTotalAmount(),
                    "UNPAID");
        }

        // The first entry is the newest — use it for status and amount
        Payment latest = payments.get(0);
        return new PaymentSummaryResponse(
                latest.getId(),
                bookingId,
                latest.getAmount(),
                booking.getTotalAmount(),
                latest.getStatus().name());
    }

    // -------------------------------------------------------------------------
    // Refund eligibility (dashboard: before showing "Trigger Refund" button)
    // -------------------------------------------------------------------------

    /**
     * Computes how much of a payment is refundable under the tenant's cancellation
     * policy at the moment this method is called.
     *
     * <p>Policy-matching logic:
     * <ol>
     *   <li>Load all CancellationPolicy tiers for the tenant, sorted descending by
     *       {@code hoursBeforeSlot} (widest window first).</li>
     *   <li>Walk the list and pick the first tier whose {@code hoursBeforeSlot}
     *       is &le; hours remaining until the booking's start time.</li>
     *   <li>If no tier matches (slot is in the past or no policy exists), the
     *       refund percentage defaults to 0.</li>
     * </ol>
     * </p>
     *
     * @param paymentId UUID of the Payment record to evaluate
     * @return refund eligibility details including exact refundable amount
     */
    @Transactional(readOnly = true)
    public RefundEligibilityResponse getRefundEligibleAmount(UUID paymentId) {
        // Load payment + booking in a single join-fetch query
        Payment payment = paymentRepository.findByIdWithBooking(paymentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment not found: " + paymentId));

        var booking = payment.getBooking();
        BigDecimal amountPaid = payment.getAmount();

        // Hours until the slot (negative means the slot is already in the past)
        long hoursUntilSlot = java.time.Duration
                .between(OffsetDateTime.now(), booking.getStartTime())
                .toHours();

        // Load the tenant's cancellation policy tiers (descending by hoursBeforeSlot)
        List<CancellationPolicy> tiers =
                cancellationPolicyRepository.findByTenantIdOrderByHoursBeforeSlotDesc(
                        booking.getTenantId());

        // Pick the first tier the current time-window satisfies
        CancellationPolicy matched = tiers.stream()
                .filter(t -> hoursUntilSlot >= t.getHoursBeforeSlot())
                .findFirst()
                .orElse(null);

        int refundPct = (matched != null) ? matched.getRefundPercentage() : 0;

        BigDecimal refundable = amountPaid
                .multiply(BigDecimal.valueOf(refundPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String description = (matched != null)
                ? refundPct + "% refund — policy applies " + matched.getHoursBeforeSlot() + "+ hours before slot"
                : "0% refund — no matching cancellation policy tier (slot may have already passed)";

        return new RefundEligibilityResponse(
                paymentId,
                booking.getId(),
                amountPaid,
                refundPct,
                refundable,
                description);
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private CustomerTransactionDetail toCustomerTransactionDetail(WalletTransaction txn) {
        // Fix 2: switch on enum — compiler catches unhandled cases
        BigDecimal balanceBefore = switch (txn.getTransactionType()) {
            case DEPOSIT, REFUND -> txn.getBalanceAfter().subtract(txn.getAmount());
            case PAYMENT         -> txn.getBalanceAfter().add(txn.getAmount());
        };

        return new CustomerTransactionDetail(
                txn.getId(),
                txn.getTransactionType().name(),
                txn.getAmount(),
                balanceBefore,
                txn.getBalanceAfter(),
                txn.getWallet().getCurrency(),
                txn.getBooking() != null ? txn.getBooking().getId() : null,
                txn.getDescription(),
                txn.getCreatedAt());
    }

    private TenantPaymentDetail toTenantPaymentDetail(Payment p) {
        return new TenantPaymentDetail(
                p.getId(),
                p.getBooking() != null ? p.getBooking().getId() : null,
                p.getStatus().name(),
                p.getPaymentMethod(),
                p.getAmount(),
                p.getCurrency(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
