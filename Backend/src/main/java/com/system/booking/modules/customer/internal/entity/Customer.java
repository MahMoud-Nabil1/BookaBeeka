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

/**
 * JPA entity representing a Customer in the booking system.
 *
 * <p><b>Isolation Guarantee:</b> Customers are global entities — they exist outside
 * the tenant ecosystem. They can book appointments with any clinic (Tenant), so they
 * deliberately inherit from {@link BaseEntity}, NOT {@code TenantBaseEntity}.
 * They have no tenantId and no role hierarchy.</p>
 *
 * <p><b>Password Storage:</b> The {@code passwordHash} field stores a BCrypt hash.
 * Plaintext passwords are never stored or logged.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "customer")
public class Customer extends BaseEntity {

    /** Email address — used as the login credential for authentication. Must be unique globally. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password — never stored or transmitted in plaintext. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 50)
    private String phone;
}
