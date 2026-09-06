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
 * Join entity linking a {@link Resource} to a {@link ServiceOffering} within the same tenant.
 *
 * <p>Like {@link ResourceAmenity}, this table carries its own {@code tenant_id} column (via
 * {@link TenantBaseEntity}), allowing efficient tenant-scoped queries on the join table
 * without requiring joins back to the parent tables.</p>
 *
 * <p>The composite unique constraint {@code uk_resource_service_link} on
 * {@code (tenant_id, resource_id, service_offering_id)} guarantees that the same service
 * cannot be linked to the same resource twice within a tenant, while still permitting
 * two different tenants' resources to be linked to logically identical services.</p>
 *
 * <p>The controller's {@code linkServiceToResource} endpoint enforces that both the
 * resource and the service offering belong to the calling tenant's ID (extracted from
 * the JWT) before saving this link — preventing cross-tenant data contamination.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "resource_service_link", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resource_service_link", columnNames = {"tenant_id", "resource_id", "service_offering_id"})
})
public class ResourceServiceLink extends TenantBaseEntity {

    // Both parent entities are pre-validated to belong to the calling tenant before this link is saved.
    // See InventoryController.linkServiceToResource().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offering_id", nullable = false)
    private ServiceOffering serviceOffering;
}