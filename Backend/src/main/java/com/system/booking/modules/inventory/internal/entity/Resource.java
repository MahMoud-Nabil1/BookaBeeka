package com.system.booking.modules.inventory.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "resource")
public class Resource extends TenantBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "capacity")
    private Integer capacity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specs", columnDefinition = "jsonb")
    private Map<String, Object> specs;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_bookable", nullable = false)
    private Boolean isBookable;

    @Column(name = "price_per_night", precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";
}
