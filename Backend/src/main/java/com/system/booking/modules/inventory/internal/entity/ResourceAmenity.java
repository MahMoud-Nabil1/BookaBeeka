package com.system.booking.modules.inventory.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Join entity linking a {@link Resource} to an {@link Amenity} within the same tenant.
 *
 * <p>Extends {@link TenantBaseEntity} so that every link row carries a {@code tenant_id}.
 * This dual-key design (tenant_id on the join table itself, plus tenant_id on both parent
 * entities) enables efficient tenant-scoped queries on the join table without requiring
 * additional joins to the parent tables — critical for performance in large multi-tenant
 * deployments.</p>
 *
 * <p>The uniqueness constraint on {@code (resource_id, amenity_id)} prevents duplicate
 * link rows. The service layer additionally validates that both the resource and the
 * amenity belong to the same tenant before creating a link, preventing cross-tenant
 * association attacks.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "resource_amenity", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"resource_id", "amenity_id"})
})
public class ResourceAmenity extends TenantBaseEntity {

    // Both sides of this association are validated to belong to the same tenant_id
    // before a link is created. See AmenityService.linkAmenityToResource().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;
}
