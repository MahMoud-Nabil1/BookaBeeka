package com.system.booking.modules.inventory.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/**
 * Represents a room category (e.g., "Deluxe Suite", "Standard Double") within a hotel tenant's inventory.
 *
 * <p><b>Schema refactoring note — removal of {@code branch_id}:</b><br>
 * In the previous schema, {@code RoomType} was scoped to a specific hotel branch via a {@code branch_id}
 * foreign key. This concept has been deprecated and fully removed. Room types are now scoped exclusively
 * at the tenant level, which reflects the business reality that a room category (and its pricing) applies
 * to the entire hotel property managed by an owner, not to individual branches.</p>
 *
 * <p><b>Unique constraint — {@code uq_room_type_tenant_name}:</b><br>
 * The composite constraint on {@code (tenant_id, name)} replaces the former branch-level uniqueness
 * constraint. This ensures no duplicate room type names exist within a tenant while allowing different
 * tenants to have identically named categories (e.g., two hotels can both have a "Deluxe" room type).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
// Scoped strictly to tenant_id. The branch_id concept has been deprecated and removed from the schema
// to enforce global tenant isolation — a room type belongs to a hotel, not to a branch within it.
@Table(name = "room_type", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_type_tenant_name", columnNames = {"tenant_id", "name"})
})
public class RoomType extends TenantBaseEntity {

    // tenant_id is inherited from TenantBaseEntity and acts as the sole isolation key.
    // All repository queries must include tenant_id to prevent cross-tenant data leakage.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "base_price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePricePerNight;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}