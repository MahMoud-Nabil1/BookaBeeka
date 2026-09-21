package com.system.booking.modules.payment.internal.repository;

import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Idempotency check — returns a completed payment if the client's
     * idempotency key was already processed, preventing double-charges on retry.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds the COMPLETED payment for a booking with a pessimistic write lock.
     * The lock prevents two concurrent refund requests from both reading COMPLETED
     * before either one writes REFUNDED (double-refund race condition fix).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId AND p.status = :status")
    Optional<Payment> findFirstByBookingIdAndStatusWithLock(
            @Param("bookingId") UUID bookingId,
            @Param("status")    PaymentStatus status);

    /**
     * All payments for every booking belonging to a tenant, paginated.
     * Includes all statuses for a complete audit trail.
     */
    Page<Payment> findByBookingTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * All payment state transitions for a single booking (PENDING→COMPLETED→REFUNDED).
     */
    List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    /**
     * Fetches a single Payment by its own ID, eagerly loading the Booking in the same
     * query. Used by the refund-eligibility check which needs booking.totalAmount and
     * booking.tenantId to compute the applicable cancellation-policy tier.
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.booking WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithBooking(@Param("paymentId") UUID paymentId);

    /**
     * Platform-wide paginated feed of payments by status — used by SuperAdmin.
     * Most common call: {@code status = FAILED} to populate the failed-payment feed.
     */
    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    /** Count of payments in a given status — used for SuperAdmin KPI stats. */
    long countByStatus(PaymentStatus status);
}
