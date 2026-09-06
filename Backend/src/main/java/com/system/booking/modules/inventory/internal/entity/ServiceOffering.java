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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a hotel service that can be linked to one or more bookable resources
 * (e.g., "Breakfast", "Airport Transfer", "Spa Package").
 *
 * <p><b>Schema refactoring note — removal of {@code branch_id}:</b><br>
 * Service offerings were previously associated with individual hotel branches via a
 * {@code branch_id} foreign key. As part of the inventory unification refactoring,
 * this concept has been removed. Service offerings are now scoped solely at the
 * tenant level, reflecting the fact that a hotel's service catalogue is a property-wide
 * concern rather than a per-branch concern.</p>
 *
 * <p><b>Unique constraint — {@code uk_service_offering_tenant_name}:</b><br>
 * Service names must be unique per tenant to prevent duplicate catalogue entries.
 * This replaces the former branch-scoped constraint. The service-to-resource linkage
 * is managed via {@link ResourceServiceLink}, which also carries a {@code tenant_id}
 * for cross-table isolation enforcement.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
// Scoped strictly to tenant_id. The branch_id concept has been deprecated and removed from the schema
// to enforce global tenant isolation — a service offering belongs to a hotel, not to a branch.
@Table(name = "service", uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_offering_tenant_name", columnNames = {"tenant_id", "name"})
})
public class ServiceOffering extends TenantBaseEntity {

    // tenant_id is inherited from TenantBaseEntity and acts as the sole isolation key.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "buffer_minutes")
    private Integer bufferMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes", columnDefinition = "jsonb")
    private Map<String, Object> customAttributes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}