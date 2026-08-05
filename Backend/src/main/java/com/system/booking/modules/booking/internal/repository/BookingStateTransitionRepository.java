package com.system.booking.modules.booking.internal.repository;

import com.system.booking.modules.booking.internal.entity.BookingStateTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingStateTransitionRepository extends JpaRepository<BookingStateTransition, UUID> {

    List<BookingStateTransition> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);
}
