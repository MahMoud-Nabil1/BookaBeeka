package com.system.booking.modules.payment.internal.repository;

import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CustomerWalletRepository extends JpaRepository<CustomerWallet, UUID> {

    /**
     * Plain read — used for balance inquiry endpoint (no lock needed for reads).
     */
    Optional<CustomerWallet> findByCustomerId(UUID customerId);

    /**
     * Fetches the customer's global wallet with a pessimistic write lock
     * (SELECT FOR UPDATE) before any balance mutation.
     *
     * One wallet per customer — no tenant scoping needed.
     * The lock prevents concurrent balance mutations (e.g. two simultaneous checkouts).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CustomerWallet w WHERE w.customer.id = :customerId")
    Optional<CustomerWallet> findByCustomerIdWithLock(@Param("customerId") UUID customerId);

    /**
     * Sum of all customer wallet balances — used by SuperAdmin platform stats
     * to show total money currently held in customer wallets across the platform.
     * COALESCE handles the empty-table case (returns 0 instead of null).
     */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM CustomerWallet w")
    BigDecimal sumAllBalances();

    /**
     * Paginated list of all customer wallets, highest balance first.
     * Used by SuperAdmin wallet overview (GET /api/admin/super/wallets/customers).
     */
    Page<CustomerWallet> findAllByOrderByBalanceDesc(Pageable pageable);
}