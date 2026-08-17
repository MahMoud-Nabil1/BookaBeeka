package com.system.booking.modules.staff.internal.entity;

/**
 * All valid role values a {@link Staff} member can hold.
 *
 * <p>Stored as a plain String in the {@code staff.role} column so that
 * no DB enum migration is needed. Use {@code StaffRole.valueOf(staff.getRole())}
 * to convert to this enum for type-safe comparisons.</p>
 *
 * <p>SUPER_ADMIN is intentionally absent — platform operators are a
 * separate user type ({@code SuperAdmin}) with their own table and
 * login endpoint, not a staff role.</p>
 */
public enum StaffRole {

    /**
     * Full tenant access. Can manage all branches, staff, services,
     * financials, and refunds within their tenant.
     */
    OWNER,

    /**
     * Branch-scoped access. Can manage bookings, staff, and reviews
     * within their assigned branch only.
     */
    ADMIN
}
