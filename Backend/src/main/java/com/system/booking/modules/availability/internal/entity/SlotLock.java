package com.system.booking.modules.availability.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.inventory.internal.entity.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a temporary soft-lock on a time slot while a customer completes checkout.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "slot_locks")
public class SlotLock extends TenantBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "slot_start", nullable = false)
    private OffsetDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private OffsetDateTime slotEnd;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
