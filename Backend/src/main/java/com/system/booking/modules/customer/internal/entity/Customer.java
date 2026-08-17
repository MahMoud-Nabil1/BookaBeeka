package com.system.booking.modules.customer.internal.entity;

import com.system.booking.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "customer")
public class Customer extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Whether this customer account is active.
     * Set to {@code false} by a SuperAdmin to ban the customer.
     * Banned customers can still have their existing data read,
     * but cannot log in (enforced at the auth layer).
     *
     * <p>DB migration: {@code ALTER TABLE customer
     * ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;}</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
