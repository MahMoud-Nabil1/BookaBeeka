package com.system.booking.modules.payment.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.booking.internal.entity.Booking;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Records a single payment transaction for a booking.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@code idempotencyKey} — client-supplied unique key preventing double-charges
 *       on network retries. Unique at DB level.</li>
 *   <li>{@code failureReason} — populated only for FAILED payments, persisted via
 *       {@code REQUIRES_NEW} so it survives the outer transaction rollback.</li>
 *   <li>{@code @Version} — optimistic lock backup behind the pessimistic wallet lock.</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "payment",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_payment_idempotency_key",
               columnNames = "idempotency_key"))
public class Payment extends TenantBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    /** Client-supplied key for idempotent checkout requests. Nullable for legacy records. */
    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    /** Human-readable reason populated only when status = FAILED. Survives rollback via REQUIRES_NEW. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Optimistic lock version — last line of defence after the pessimistic wallet lock. */
    @Version
    @Column(name = "version")
    private Long version;
}
