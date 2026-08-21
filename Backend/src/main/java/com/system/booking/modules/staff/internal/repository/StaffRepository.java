package com.system.booking.modules.staff.internal.repository;

import com.system.booking.modules.staff.internal.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Staff} entity persistence.
 *
 * <p>This repository is accessed internally by multiple consumers:</p>
 * <ul>
 *   <li>{@code StaffSecurityAdapter} — <b>Read-only</b> operations for authentication</li>
 *   <li>{@code StaffProvisioningAdapter} — <b>Write-only</b> operations for owner staff creation</li>
 *   <li>{@code StaffService} — <b>CRUD</b> operations for tenant-scoped staff management</li>
 * </ul>
 *
 * <p><b>Tenant Isolation:</b> Methods suffixed with {@code AndTenantId} enforce
 * multi-tenant data scoping. The {@code StaffService} always passes the current
 * tenant's UUID (extracted from {@code SecurityUtil}) to these methods, guaranteeing
 * that no cross-tenant data leakage can occur.</p>
 */
@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    /**
     * Finds a staff member by their email address (global lookup).
     * Used by {@code StaffSecurityAdapter} for authentication lookup.
     */
    Optional<Staff> findByEmail(String email);

    /**
     * Checks whether a staff member with the given email already exists (global).
     * Used by {@code StaffProvisioningAdapter} to prevent duplicate registrations.
     */
    boolean existsByEmail(String email);

    // ── Tenant-Scoped Queries (used by StaffService for CRUD) ───────────────

    /**
     * Lists all staff members belonging to a specific tenant.
     * Used by {@code StaffService#listStaffForCurrentTenant()}.
     *
     * @param tenantId the tenant UUID to scope the query
     * @return all staff within the given tenant
     */
    List<Staff> findAllByTenantId(UUID tenantId);

    /**
     * Finds a staff member by ID, scoped to a specific tenant.
     * Prevents cross-tenant data access — even if a valid staff UUID is provided,
     * it will only match if the staff belongs to the specified tenant.
     *
     * @param id       the staff member's UUID
     * @param tenantId the tenant UUID to scope the query
     * @return the staff member if found within the tenant, empty otherwise
     */
    Optional<Staff> findByIdAndTenantId(UUID id, UUID tenantId);
}
