package com.system.booking.modules.payment.internal.repository;

import com.system.booking.modules.payment.internal.entity.TenantWallet;
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

public interface TenantWalletRepository extends JpaRepository<TenantWallet, UUID> {

    /**
     * Fetches the tenant's revenue wallet for a simple read (no lock).
     * Used for balance inquiries and history endpoints.
     */
    Optional<TenantWallet> findByTenantId(UUID tenantId);

    /**
     * Fetches the tenant's revenue wallet with a pessimistic write lock
     * (SELECT FOR UPDATE) before any credit or debit operation.
     *
     * Prevents two concurrent payments for the same tenant from producing
     * an incorrect final balance due to a lost update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tw FROM TenantWallet tw WHERE tw.tenant.id = :tenantId")
    Optional<TenantWallet> findByTenantIdWithLock(@Param("tenantId") UUID tenantId);

    /** Paginated list of all tenant wallets — used by SuperAdmin wallet overview. */
    Page<TenantWallet> findAllByOrderByBalanceDesc(Pageable pageable);

    /**
     * Sum of all tenant revenue balances — used by SuperAdmin platform stats
     * to show total revenue accumulated across all tenant wallets.
     */
    @Query("SELECT COALESCE(SUM(tw.balance), 0) FROM TenantWallet tw")
    BigDecimal sumAllBalances();
}
