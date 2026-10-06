package com.system.booking.modules.review.internal.repository;

import com.system.booking.modules.review.internal.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // enforces one review per booking at the service layer
    boolean existsByBookingId(UUID bookingId);

    // tenant-scoped lookup — used by ADMIN/OWNER replies (never trust a bare findById for them)
    Optional<Review> findByIdAndTenantId(UUID id, UUID tenantId);

    // public-facing listing for a given service within a tenant
    Page<Review> findByTenantIdAndServiceId(UUID tenantId, UUID serviceId, Pageable pageable);

    // room-based listing for customer-facing page — reviews where room_id matches
    Page<Review> findByTenantIdAndRoomId(UUID tenantId, UUID roomId, Pageable pageable);

    // fallback: all reviews for this room across any tenant (for catalog page)
    Page<Review> findByRoomId(UUID roomId, Pageable pageable);

    // admin/owner dashboard — all reviews for their hotel; also used by SUPER_ADMIN with an explicit tenantId
    Page<Review> findByTenantId(UUID tenantId, Pageable pageable);

    // a customer's own review history, across every tenant they've booked with
    Page<Review> findByCustomerId(UUID customerId, Pageable pageable);
}
