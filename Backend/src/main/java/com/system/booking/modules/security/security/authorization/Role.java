package com.system.booking.modules.security.security.authorization;

public final class Role {

    private Role() {
        // Utility class — prevent instantiation
    }

    // ── Platform SuperAdmin ─────────────────────────────────────────────────────
    /** Platform-level superuser who manages tenants. */
    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    // ── Hotel / Tenant Roles ────────────────────────────────────────────────────
    /** Hotel owner with full operational and financial access to their tenant. */
    public static final String OWNER = "ROLE_OWNER";

    /** Hotel administrator / Front desk who manages rooms and bookings. */
    public static final String ADMIN = "ROLE_ADMIN";

    // ── Customer Ecosystem Role ─────────────────────────────────────────────────
    /** External guest / customer — completely isolated from the tenant ecosystem. */
    public static final String CUSTOMER = "ROLE_CUSTOMER";
}