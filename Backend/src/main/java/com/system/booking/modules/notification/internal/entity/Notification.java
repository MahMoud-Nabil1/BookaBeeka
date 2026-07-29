package com.system.booking.modules.notification.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "notification")
public class Notification extends TenantBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
