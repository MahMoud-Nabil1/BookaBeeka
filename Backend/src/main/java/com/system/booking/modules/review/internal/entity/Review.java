package com.system.booking.modules.review.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

// maps 1:1 onto the existing `review` table (id/created_at/updated_at/tenant_id via TenantBaseEntity)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "review")
public class Review extends TenantBaseEntity {

    // one review per booking — enforced by a DB unique constraint too (see migration)
    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    // service offering this review is about — nullable because not all bookings have an
    // explicit service offering (e.g. room-only bookings with no ResourceServiceLink).
    @Column(name = "service_id")
    private UUID serviceId;

    // which staff member the review is about, if applicable — separate from who replies
    @Column(name = "staff_id")
    private UUID staffId;

    // the room (resource) this review is about — set from the booking's resource_id at creation time.
    // Enables direct room-based review queries even when serviceId is null.
    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    // true because it's only ever created off a real, completed booking
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    // --- staff reply, available to ADMIN / OWNER / SUPER_ADMIN, editable anytime ---

    @Column(name = "reply", columnDefinition = "text")
    private String reply;

    @Column(name = "replied_by")
    private UUID repliedBy;

    // ADMIN / OWNER / SUPER_ADMIN — kept because staff_id alone doesn't tell you the role
    @Column(name = "replied_by_role", length = 20)
    private String repliedByRole;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;
}
