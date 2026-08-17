package com.system.booking.modules.admin.internal.entity;

import com.system.booking.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Platform-level administrator. Completely separate from {@code Staff} —
 * SuperAdmins are not scoped to any tenant and have cross-tenant visibility.
 *
 * <p>DB migration:
 * <pre>{@code
 * CREATE TABLE super_admin (
 *   id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
 *   email        VARCHAR(255) NOT NULL UNIQUE,
 *   password_hash VARCHAR(255) NOT NULL,
 *   first_name   VARCHAR(100) NOT NULL,
 *   last_name    VARCHAR(100) NOT NULL,
 *   is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
 *   created_at   TIMESTAMP   NOT NULL,
 *   updated_at   TIMESTAMP   NOT NULL
 * );
 * }</pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "super_admin")
public class SuperAdmin extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Whether this super-admin account is enabled.
     * Disabled accounts are rejected at login even with valid credentials.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
