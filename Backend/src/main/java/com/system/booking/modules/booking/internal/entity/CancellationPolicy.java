package com.system.booking.modules.booking.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// tenant-level refund tiers (e.g. 100% if cancelled 48h+ before, 50% within 24h, 0% day-of)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "cancellation_policy")
public class CancellationPolicy extends TenantBaseEntity {

    // how many hours before the slot this tier kicks in
    @Column(name = "hours_before_slot", nullable = false)
    private Integer hoursBeforeSlot;

    // what percentage of the total gets refunded
    @Column(name = "refund_percentage", nullable = false)
    private Integer refundPercentage;
}
