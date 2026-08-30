package com.system.booking.modules.payment.internal.entity;

import com.system.booking.common.model.BaseEntity;
import com.system.booking.modules.booking.internal.entity.Booking;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Immutable ledger entry recording every balance change on a CustomerWallet.
 *
 * Tenant context is NOT stored directly — for PAYMENT and REFUND entries it
 * can be resolved via the linked Booking (which is tenant-scoped). DEPOSIT
 * entries have no booking and therefore no tenant context, which is correct:
 * the customer is simply adding funds to their global wallet.
 *
 * The {@code payment} FK is nullable:
 * <ul>
 *   <li>DEPOSIT entries have no originating Payment (null by design).</li>
 *   <li>PAYMENT and REFUND entries link to the exact Payment record that
 *       caused the deduction / reversal, closing the audit trail gap.
 *       Without this link you could only say "this deduction belongs to
 *       booking X" but not "it was caused by payment record Y".</li>
 * </ul>
 *
 * No {@code @Version} needed — ledger entries are append-only (never updated).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CustomerWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    /**
     * The Payment record that triggered this ledger entry.
     * Null for DEPOSIT entries (no payment involved); always set for PAYMENT and REFUND.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;
}
