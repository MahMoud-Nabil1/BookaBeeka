package com.system.booking.modules.inventory.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a bookable physical unit within a hotel tenant's inventory (e.g., a specific room,
 * meeting room, or other bookable space).
 *
 * <p><b>Schema refactoring note — removal of {@code branch_id}:</b><br>
 * Resources were previously tied to a hotel branch via a {@code branch_id} foreign key,
 * which introduced unnecessary complexity when querying and filtering inventory. Following
 * the architectural decision to unify tenant-level isolation, {@code branch_id} has been
 * fully removed. All data access is now scoped solely through {@code tenant_id}, which is
 * extracted from the authenticated user's JWT — never from the request payload.</p>
 *
 * <p><b>Unique constraint — {@code uk_resource_tenant_room_number}:</b><br>
 * Room numbers must be unique within a tenant (i.e., a hotel cannot have two rooms numbered "101").
 * This replaces the former branch-scoped uniqueness constraint, ensuring integrity at the
 * hotel-property level.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
// Scoped strictly to tenant_id. The branch_id concept has been deprecated and removed from the schema
// to enforce global tenant isolation — a resource belongs to a hotel, not to a branch within it.
@Table(name = "resource", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resource_tenant_room_number", columnNames = {"tenant_id", "room_number"})
})
public class Resource extends TenantBaseEntity {

    // tenant_id is inherited from TenantBaseEntity and acts as the sole isolation key.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    // The RoomType association is validated tenant-locally: when creating or updating a Resource,
    // the service layer ensures the referenced RoomType belongs to the same tenant_id.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "resource_type", nullable = false, length = 50)
    @Builder.Default
    private String resourceType = "ROOM";

    @Column(name = "capacity")
    private Integer capacity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specs", columnDefinition = "jsonb")
    private Map<String, Object> specs;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_bookable", nullable = false)
    @Builder.Default
    private Boolean isBookable = true;

    @Column(name = "price_per_night", precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";
}