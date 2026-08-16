package com.system.booking.modules.security.security.authorization;

/**
 * Centralized Spring Security role constants used across the entire security infrastructure.
 *
 * <p>These constants follow the Spring Security convention of prefixing roles with "ROLE_"
 * so they work seamlessly with {@code hasRole()} and {@code @PreAuthorize} expressions.</p>
 *
 * <h3>Role Hierarchy (Tenant Ecosystem):</h3>
 * <pre>
 *   SUPER_ADMIN  →  Platform-level superuser; can onboard new tenants.
 *   OWNER        →  Full access within their tenant (clinic/business).
 *   ADMIN        →  Manages bookings, staff, and tenant settings.
 *   STAFF        →  Handles only assigned tasks within the tenant.
 * </pre>
 *
 * <h3>Isolated Role (Customer Ecosystem):</h3>
 * <pre>
 *   CUSTOMER     →  Public-facing user; completely isolated from the tenant ecosystem.
 * </pre>
 *
 * <p><b>Design Note:</b> Roles are stored as plain strings (e.g., "OWNER") in the {@code staff.role}
 * database column and mapped to Spring Security authorities with the "ROLE_" prefix at runtime.</p>
 */
public final class Role {

    private Role() {
        // Utility class — prevent instantiation
    }

    // ── Tenant Ecosystem Roles ──────────────────────────────────────────────────

    /** Platform-level superuser who can register new tenants. */
    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    /** Tenant owner with full access to all resources within their tenant. */
    public static final String OWNER = "ROLE_OWNER";

    /** Tenant administrator who manages bookings, staff, and settings. */
    public static final String ADMIN = "ROLE_ADMIN";

    /** Tenant staff member who handles only assigned tasks. */
    public static final String STAFF = "ROLE_STAFF";

    // ── Customer Ecosystem Role ─────────────────────────────────────────────────

    /** External customer — completely isolated from the tenant/staff ecosystem. */
    public static final String CUSTOMER = "ROLE_CUSTOMER";
}
