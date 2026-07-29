package com.system.booking.modules.payment.api;

import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import com.system.booking.modules.payment.internal.service.PaymentHistoryService;
import com.system.booking.modules.payment.internal.service.WalletPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adapter that implements {@link PaymentModuleApi} by delegating to the
 * internal {@link WalletPaymentService} / {@link PaymentHistoryService} and
 * mapping entity results to public-facing DTOs.
 *
 * <p>This class is the only place where the {@code payment.internal} package
 * is referenced from within the {@code api} package, keeping the boundary
 * clean and predictable.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentModuleApiImpl implements PaymentModuleApi {

    private final WalletPaymentService  walletPaymentService;
    private final PaymentHistoryService paymentHistoryService;

    // -------------------------------------------------------------------------
    // Interface implementation — writes
    // -------------------------------------------------------------------------

    @Override
    public WalletTransactionResponse topUpWallet(UUID customerId, BigDecimal amount) {
        WalletTransaction txn = walletPaymentService.topUpWallet(customerId, amount);
        return toTransactionResponse(txn);
    }

    @Override
    public PaymentResponse processPayment(UUID bookingId, UUID customerId, UUID tenantId, BigDecimal paymentAmount,
                                          String idempotencyKey, String expectedCurrency) {
        Payment payment = walletPaymentService.processPayment(
                bookingId, customerId, tenantId, paymentAmount, idempotencyKey, expectedCurrency);
        return toPaymentResponse(payment);
    }

    @Override
    public PaymentResponse refundPayment(UUID bookingId, UUID customerId, UUID tenantId) {
        Payment payment = walletPaymentService.refundPayment(bookingId, customerId, tenantId);
        return toPaymentResponse(payment);
    }

    // -------------------------------------------------------------------------
    // Interface implementation — reads
    // -------------------------------------------------------------------------

    @Override
    public CustomerBalanceResponse getCustomerBalance(UUID customerId) {
        return paymentHistoryService.getCustomerBalance(customerId);
    }

    @Override
    public Page<CustomerTransactionDetail> getCustomerHistory(UUID customerId, int page, int size) {
        return paymentHistoryService.getCustomerHistory(customerId, page, size);
    }

    @Override
    public Page<TenantPaymentDetail> getTenantPaymentHistory(UUID tenantId, int page, int size) {
        return paymentHistoryService.getTenantPaymentHistory(tenantId, page, size);
    }

    @Override
    public TenantBalanceResponse getTenantBalance(UUID tenantId) {
        return paymentHistoryService.getTenantBalance(tenantId);
    }

    // -------------------------------------------------------------------------
    // Private mappers — entity → DTO
    // Kept here (not in the entity) to preserve internal/api separation.
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link Payment} JPA entity to its public DTO representation.
     * Null-safe on {@code booking} in case of an edge-case detached reference.
     */
    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking() != null ? payment.getBooking().getId() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                payment.getCreatedAt()
        );
    }

    /**
     * Maps a {@link WalletTransaction} JPA entity to its public DTO representation.
     * The {@code bookingId} is null for DEPOSIT transactions by design.
     */
    private WalletTransactionResponse toTransactionResponse(WalletTransaction txn) {
        return new WalletTransactionResponse(
                txn.getId(),
                txn.getWallet().getId(),
                txn.getBooking() != null ? txn.getBooking().getId() : null,
                txn.getAmount(),
                txn.getTransactionType().name(),
                txn.getBalanceAfter(),
                txn.getDescription(),
                txn.getCreatedAt()
        );
    }
}