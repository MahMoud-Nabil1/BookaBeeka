package com.system.booking.modules.payment.internal.service;

import com.system.booking.modules.payment.api.CustomerBalanceResponse;
import com.system.booking.modules.payment.api.CustomerTransactionDetail;
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
