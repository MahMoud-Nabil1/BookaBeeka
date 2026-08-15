package com.system.booking.modules.staff.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.tenant.internal.entity.Branch;
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

/**
 * JPA entity representing a staff member within the multi-tenant booking system.
 *
 * <p>Staff members are always scoped to a specific {@link Tenant} (inherited via
 * {@link TenantBaseEntity#getTenantId()}) and assigned to a specific {@link Branch}.
 * The one exception is the SUPER_ADMIN, which has a {@code null} tenantId since
 * they operate at the platform level.</p>
 *
 * <h3>Role Assignments:</h3>
 * <ul>
 *   <li><b>SUPER_ADMIN</b> — Platform-level; can onboard new tenants (no tenant affiliation)</li>
 *   <li><b>OWNER</b>       — Full access within their tenant</li>
 *   <li><b>ADMIN</b>       — Manages bookings, staff, and settings within their tenant</li>
 *   <li><b>STAFF</b>       — Handles only assigned tasks within their branch</li>
 * </ul>
 *
 * <p><b>Security Note:</b> Roles are stored as plain strings (e.g., "OWNER") and
 * mapped to Spring Security authorities with the "ROLE_" prefix at runtime by
 * the {@code JwtAuthenticationFilter}.</p>
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
@Table(name = "staff")
public class Staff extends TenantBaseEntity {

    /** Lazy-loaded reference to the Tenant entity (read-only mapping). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    /** The branch this staff member is assigned to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Email address — used as the login credential for authentication. */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Staff role as a plain string (e.g., "OWNER", "ADMIN", "STAFF", "SUPER_ADMIN").
     * Mapped to Spring Security authority with "ROLE_" prefix at runtime.
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /** Account status flag — inactive accounts are rejected during authentication. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /** BCrypt-hashed password — never stored or transmitted in plaintext. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
