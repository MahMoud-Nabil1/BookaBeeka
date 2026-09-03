package com.system.booking.modules.booking.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "booking")
public class Booking extends TenantBaseEntity {

    // stripped to plain UUIDs to respect module boundaries
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "service_offering_id")
    private UUID serviceOfferingId;

    // references the slot lock used during checkout
    @Column(name = "lock_id")
    private UUID lockId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "source", length = 50)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    // hibernate uses this for optimistic locking
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;
}
