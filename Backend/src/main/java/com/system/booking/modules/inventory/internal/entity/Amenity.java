package com.system.booking.modules.inventory.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a feature or facility that can be linked to one or more bookable resources
 * (e.g., "WiFi", "King Bed", "Jacuzzi", "Sea View").
 *
 * <p><b>Schema refactoring note — removal of {@code branch_id}:</b><br>
 * Amenities were previously associated with specific hotel branches. Following the
 * inventory refactoring, this association has been fully removed. Amenities are now
 * scoped at the tenant level — they represent a hotel-wide catalogue of features
 * that can be linked to individual rooms/resources via {@link ResourceAmenity}.</p>
 *
 * <p><b>Unique constraint — {@code uk_amenity_tenant_name}:</b><br>
 * Amenity names must be unique per tenant to avoid duplicate catalogue entries.
 * This replaces the former branch-scoped constraint. The new constraint correctly
 * allows different hotel tenants to independently define identically named amenities
 * (e.g., both Hotel A and Hotel B can have a "Free WiFi" amenity).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
// Scoped strictly to tenant_id. The branch_id concept has been deprecated and removed from the schema
// to enforce global tenant isolation — an amenity belongs to a hotel, not to a branch.
@Table(name = "amenity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_amenity_tenant_name", columnNames = {"tenant_id", "name"})
})
public class Amenity extends TenantBaseEntity {

    // tenant_id is inherited from TenantBaseEntity and acts as the sole isolation key.
    // All repository queries must include tenant_id to prevent cross-tenant data leakage.
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}