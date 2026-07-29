package com.system.booking.modules.payment.internal.repository;

import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    /**
     * Returns a paginated view of a customer's wallet ledger, newest-first.
     * Paginated to prevent OutOfMemoryError for high-volume customers.
     */
    Page<WalletTransaction> findByWalletCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    /**
     * Returns a paginated view of all entries for a specific wallet by wallet ID, newest-first.
     * Not currently called anywhere in the module — kept as a ready-made building block
     * for future internal use (e.g. wallet-level admin tooling). Remove if it stays
     * unused, since dead repository methods are easy to lose track of.
     */
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}