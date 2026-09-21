package com.system.booking.modules.booking.internal.repository;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    /** Count of bookings for a tenant — used by SuperAdmin tenant detail view. */
    long countByTenantId(UUID tenantId);

    /** Count of bookings for a tenant in a given status — used by SuperAdmin tenant detail view. */
    long countByTenantIdAndStatus(UUID tenantId, BookingStatus status);

    Optional<Booking> findByTenantIdAndId(UUID tenantId, UUID id);

    List<Booking> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    // for the expiry sweep — finds stale pending bookings
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);

    // for the completion sweep — finds confirmed bookings past their end time
    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, OffsetDateTime cutoff);

    /**
     * Paginated stuck-booking detector for SuperAdmin.
     * Returns PENDING_PAYMENT bookings older than {@code cutoff} — these should have
     * either been paid or expired by now, indicating a potential processing issue.
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.createdAt < :cutoff ORDER BY b.createdAt ASC")
    Page<Booking> findStuckBookings(
            @Param("status") BookingStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);

    /** Count of bookings in a given status — used for SuperAdmin platform KPI stats. */
    long countByStatus(BookingStatus status);
}
