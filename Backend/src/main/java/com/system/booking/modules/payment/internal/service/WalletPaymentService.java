package com.system.booking.modules.payment.internal.service;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.PaymentStatus;
import com.system.booking.modules.payment.internal.entity.TenantWallet;
import com.system.booking.modules.payment.internal.entity.TransactionType;
import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import com.system.booking.modules.payment.internal.exception.InsufficientBalanceException;
import com.system.booking.modules.payment.internal.repository.CustomerWalletRepository;
import com.system.booking.modules.payment.internal.repository.PaymentRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.payment.internal.repository.WalletTransactionRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service orchestrating all wallet-based financial operations.
 *
 * <p>Every public method is wrapped in a single {@code @Transactional(timeout = 10)}
 * boundary — 10 seconds is enough for all DB work; if exceeded, the transaction is
 * rolled back and the pessimistic lock on the wallet is released, preventing lock storms.</p>
 *
 * <p>All status and transaction-type values use enums — no magic strings.</p>
 *
 * <p><strong>Known limitation (flagged, not yet fixed):</strong> {@code tenantId} in
 * {@link #processPayment} and {@link #refundPayment} is trusted as supplied by the
 * caller (ultimately the HTTP request body via {@code PaymentController}). Nothing
 * here verifies it actually matches the tenant that owns {@code bookingId}, so a
 * caller could credit/debit the wrong tenant's revenue wallet by sending a mismatched
 * {@code tenantId} alongside a valid {@code bookingId}. The correct fix is to derive
 * the tenant from the booking itself (e.g. {@code booking.getTenant().getId()}) and
 * either ignore the caller-supplied value or reject the request if it disagrees —
 * but that requires the {@code Booking} entity/repository, which wasn't part of this
 * review batch. Wire this up once those are available.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletPaymentService {

    private static final String PAYMENT_METHOD_WALLET = "WALLET";

    /** Configurable max top-up ceiling. Override via application.properties: payment.wallet.max-balance */
    @Value("${payment.wallet.max-balance:100000.00}")
    private BigDecimal maxWalletBalance;

    private final EntityManager               entityManager;
    private final PaymentRepository           paymentRepository;
    private final CustomerWalletRepository    customerWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TenantWalletRepository      tenantWalletRepository;
    private final PaymentAuditService         paymentAuditService;

    // -------------------------------------------------------------------------
    // Scenario 1: Wallet Top-Up (Deposit)
    // -------------------------------------------------------------------------

    /**
     * Credits the customer's global wallet with the given amount.
     *
     * @param customerId the customer to credit
     * @param amount     positive amount to add
     * @return persisted WalletTransaction ledger entry
     * @throws IllegalArgumentException if amount is negative/zero or would exceed max balance
     * @throws EntityNotFoundException  if no wallet exists for the customer
     */
    @Transactional(timeout = 10)
    public WalletTransaction topUpWallet(UUID customerId, BigDecimal amount) {

        validatePositiveAmount(amount, "Top-up amount");

        log.info("Top-up request for customer [{}], amount: {}", customerId, amount);

        CustomerWallet wallet = findWalletWithLock(customerId);

        // Fix 10: Maximum balance cap — prevents abuse / runaway credits
        BigDecimal newBalance = wallet.getBalance().add(amount);
        if (newBalance.compareTo(maxWalletBalance) > 0) {
            throw new IllegalArgumentException(
                    String.format("Top-up rejected: resulting balance %.2f would exceed the maximum of %.2f %s",
                            newBalance, maxWalletBalance, wallet.getCurrency()));
        }

        wallet.setBalance(newBalance);
        customerWalletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(TransactionType.DEPOSIT)  // Fix 2: enum
                .balanceAfter(newBalance)
                .description("Wallet top-up")
                .build();

        WalletTransaction saved = walletTransactionRepository.save(txn);
        log.info("Top-up complete for customer [{}]. New balance: {} {}", customerId, newBalance, wallet.getCurrency());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Checkout / Payment Flow
    // -------------------------------------------------------------------------

    /**
     * Attempts to pay for a booking using the customer's wallet balance.
     *
     * <p>Idempotency: if {@code idempotencyKey} is supplied and a payment with that
     * key already exists (COMPLETED or otherwise), it is returned immediately without
     * re-processing — safe for client retries after network failures.</p>
     *
     * <p>Currency guard: if {@code expectedCurrency} is supplied, it must match the
     * wallet's configured currency or the payment is rejected immediately.</p>
     *
     * @param bookingId        booking being paid for
     * @param customerId       paying customer
     * @param tenantId         tenant context (stored on Payment) — see class-level note
     *                         on the trust limitation of this parameter
     * @param paymentAmount    exact amount to charge
     * @param idempotencyKey   client-supplied key preventing double-charges on retry (nullable)
     * @param expectedCurrency ISO-4217 code the caller expects the wallet to be in (nullable)
     * @return COMPLETED Payment record
     */
    @Transactional(timeout = 10)
    public Payment processPayment(UUID bookingId, UUID customerId, UUID tenantId,
                                  BigDecimal paymentAmount,
                                  String idempotencyKey, String expectedCurrency) {

        // Fix 1: Idempotency — return existing result without re-processing
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent checkout: key [{}] already processed. Returning existing payment.", idempotencyKey);
                return existing.get();
            }
        }

        validatePositiveAmount(paymentAmount, "Payment amount");

        log.info("Checkout for booking [{}], customer [{}], amount: {}", bookingId, customerId, paymentAmount);

        // Step 1: Lock wallet — early lock keeps the wallet consistent for the full transaction
        CustomerWallet wallet = findWalletWithLock(customerId);

        // Fix 5: Currency mismatch — reject before any DB writes
        if (expectedCurrency != null && !wallet.getCurrency().equalsIgnoreCase(expectedCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: wallet is %s but payment expects %s",
                            wallet.getCurrency(), expectedCurrency));
        }

        // Step 2: Create PENDING payment — rolled back on failure (that is intentional)
        // NOTE: tenantId is trusted as-is here — see class-level javadoc "Known limitation".
        Booking bookingRef = buildBookingRef(bookingId);
        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .booking(bookingRef)
                .amount(paymentAmount)
                .status(PaymentStatus.PENDING)       // Fix 2: enum
                .paymentMethod(PAYMENT_METHOD_WALLET)
                .currency(wallet.getCurrency())
                .idempotencyKey(idempotencyKey)      // Fix 1: stored for future idempotency checks
                .build();
        payment = paymentRepository.save(payment);

        // Step 3: Balance check
        BigDecimal currentBalance = wallet.getBalance();

        if (currentBalance.compareTo(paymentAmount) >= 0) {
            // Sufficient — deduct, ledger, complete
            BigDecimal newBalance = currentBalance.subtract(paymentAmount);
            wallet.setBalance(newBalance);
            customerWalletRepository.save(wallet);

            walletTransactionRepository.save(WalletTransaction.builder()
                    .wallet(wallet)
                    .booking(bookingRef)
                    .amount(paymentAmount)
                    .transactionType(TransactionType.PAYMENT)  // Fix 2: enum
                    .balanceAfter(newBalance)
                    .description("Payment for booking " + bookingId)
                    .build());

            payment.setStatus(PaymentStatus.COMPLETED);       // Fix 2: enum
            payment = paymentRepository.save(payment);

            creditTenantWallet(tenantId, paymentAmount, wallet.getCurrency());

            log.info("Payment COMPLETED for booking [{}]. Customer balance: {}", bookingId, newBalance);
            return payment;
        }

        // Insufficient balance — Fix 7: audit record survives rollback via REQUIRES_NEW
        String reason = String.format("Required: %.2f, Available: %.2f %s",
                paymentAmount, currentBalance, wallet.getCurrency());

        paymentAuditService.recordFailedPayment(bookingId, tenantId, paymentAmount,
                wallet.getCurrency(), reason);

        log.warn("Payment FAILED for booking [{}]. {}", bookingId, reason);
        throw new InsufficientBalanceException(customerId, paymentAmount, currentBalance);
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Cancellation & Refund Flow
    // -------------------------------------------------------------------------

    /**
     * Reverses a completed payment. Uses a pessimistic lock on the Payment record
     * to prevent two concurrent refund requests from both reading COMPLETED before
     * either writes REFUNDED (double-refund race condition).
     *
     * @param bookingId  booking to refund
     * @param customerId customer to credit
     * @param tenantId   tenant context — see class-level note on the trust limitation
     *                   of this parameter
     * @return REFUNDED Payment record
     */
    @Transactional(timeout = 10)
    public Payment refundPayment(UUID bookingId, UUID customerId, UUID tenantId) {

        log.info("Refund request for booking [{}], customer [{}]", bookingId, customerId);

        // Fix 6: Locked fetch — prevents double-refund race condition
        Payment payment = paymentRepository
                .findFirstByBookingIdAndStatusWithLock(bookingId, PaymentStatus.COMPLETED)  // Fix 2: enum
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No COMPLETED payment found for booking [%s]. Refund aborted.", bookingId)));

        BigDecimal refundAmount = payment.getAmount();

        CustomerWallet wallet = findWalletWithLock(customerId);

        BigDecimal newBalance = wallet.getBalance().add(refundAmount);

        // The max-balance cap (Fix 10) intentionally does NOT block refunds: a customer
        // must always get their money back even if that pushes them past the configured
        // ceiling. We only log so the situation is visible/auditable, not silently hidden.
        if (newBalance.compareTo(maxWalletBalance) > 0) {
            log.warn("Refund for booking [{}] brings customer [{}] balance to {} {}, which exceeds the " +
                            "configured max of {} — proceeding anyway since refunds must never be blocked.",
                    bookingId, customerId, newBalance, wallet.getCurrency(), maxWalletBalance);
        }

        wallet.setBalance(newBalance);
        customerWalletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .booking(buildBookingRef(bookingId))
                .amount(refundAmount)
                .transactionType(TransactionType.REFUND)     // Fix 2: enum
                .balanceAfter(newBalance)
                .description("Refund for cancelled booking " + bookingId)
                .build());

        payment.setStatus(PaymentStatus.REFUNDED);           // Fix 2: enum
        payment = paymentRepository.save(payment);

        debitTenantWallet(tenantId, refundAmount);

        log.info("Refund COMPLETED for booking [{}]. Amount: {}, Customer new balance: {}",
                bookingId, refundAmount, newBalance);
        return payment;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private CustomerWallet findWalletWithLock(UUID customerId) {
        return customerWalletRepository
                .findByCustomerIdWithLock(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Wallet not found for customer: " + customerId));
    }

    private Booking buildBookingRef(UUID bookingId) {
        return entityManager.getReference(Booking.class, bookingId);
    }

    private void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive. Received: " + amount);
        }
    }

    private void creditTenantWallet(UUID tenantId, BigDecimal amount, String currency) {
        TenantWallet tenantWallet = findOrCreateTenantWallet(tenantId, currency);
        tenantWallet.setBalance(tenantWallet.getBalance().add(amount));
        tenantWalletRepository.save(tenantWallet);
    }

    private void debitTenantWallet(UUID tenantId, BigDecimal refundAmount) {
        tenantWalletRepository.findByTenantIdWithLock(tenantId).ifPresent(tw -> {
            tw.setBalance(tw.getBalance().subtract(refundAmount).max(BigDecimal.ZERO));
            tenantWalletRepository.save(tw);
        });
    }

    private TenantWallet findOrCreateTenantWallet(UUID tenantId, String currency) {
        return tenantWalletRepository.findByTenantIdWithLock(tenantId)
                .orElseGet(() -> {
                    log.info("Auto-creating revenue wallet for tenant [{}]", tenantId);
                    return tenantWalletRepository.save(TenantWallet.builder()
                            .tenant(entityManager.getReference(Tenant.class, tenantId))
                            .balance(BigDecimal.ZERO)
                            .currency(currency)
                            .build());
                });
    }
}