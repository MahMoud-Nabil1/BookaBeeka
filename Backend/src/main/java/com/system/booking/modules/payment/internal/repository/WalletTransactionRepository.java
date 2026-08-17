package com.system.booking.modules.payment.internal.repository;

import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Platform-wide paginated transaction feed for SuperAdmin.
     * Eagerly loads wallet→customer (for FROM name) and optional booking (for TO tenant)
     * in a single query to prevent N+1 when rendering the full audit table.
     *
     * LEFT JOIN on booking and payment because DEPOSIT entries have neither.
     */
    @Query("SELECT wt FROM WalletTransaction wt " +
            "JOIN FETCH wt.wallet w " +
            "JOIN FETCH w.customer c " +
            "LEFT JOIN FETCH wt.booking b " +
            "LEFT JOIN FETCH wt.payment p " +
            "ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findAllWithDetailsOrderByCreatedAtDesc(Pageable pageable);
}