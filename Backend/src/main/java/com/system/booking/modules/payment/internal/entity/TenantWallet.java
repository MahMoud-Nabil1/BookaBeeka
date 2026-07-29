package com.system.booking.modules.payment.internal.entity;

import com.system.booking.common.model.BaseEntity;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Tracks a tenant's accumulated revenue from completed bookings.
 *
 * Auto-created on the first completed payment — tenants never register manually.
 * Debited on refunds. Balance is floored at zero to avoid negative values in
 * edge cases (e.g. manual adjustments outside the system).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tenant_wallet",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_tenant_wallet_tenant_id",
               columnNames = "tenant_id"))
public class TenantWallet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Version
    @Column(name = "version")
    private Long version;
}
