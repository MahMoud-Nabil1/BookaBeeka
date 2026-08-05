package com.system.booking.modules.booking.internal.repository;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByTenantIdAndId(UUID tenantId, UUID id);

    List<Booking> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    // for the expiry sweep — finds stale pending bookings
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);

    // for the completion sweep — finds confirmed bookings past their end time
    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, OffsetDateTime cutoff);
}
