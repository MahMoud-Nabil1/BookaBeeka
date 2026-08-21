package com.system.booking.modules.staff.internal.service;

import com.system.booking.modules.security.util.SecurityUtil;
import com.system.booking.modules.staff.internal.dto.request.CreateStaffRequest;
import com.system.booking.modules.staff.internal.dto.request.UpdateStaffRequest;
import com.system.booking.modules.staff.internal.dto.response.StaffResponse;
import com.system.booking.modules.staff.internal.entity.Staff;
import com.system.booking.modules.staff.internal.repository.StaffRepository;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Core business service for managing Staff members within a tenant.
 *
 * <h3>Tenant Isolation Strategy:</h3>
 * <p>Every method in this service extracts the current tenant's UUID from
 * {@link SecurityUtil#getCurrentStaffPrincipal()} and uses it to scope all
 * database queries. This guarantees that:</p>
 * <ul>
 *   <li>Staff members from other tenants are <b>never</b> visible</li>
 *   <li>Cross-tenant modifications are <b>impossible</b></li>
 *   <li>The isolation is enforced at the <b>service layer</b>, not just the API layer</li>
 * </ul>
 *
 * <h3>Separation from Adapters:</h3>
 * <p>This service handles <b>CRUD operations</b> for tenant-scoped staff management.
 * It is separate from:</p>
 * <ul>
 *   <li>{@code StaffSecurityAdapter} — read-only authentication lookups (Security port)</li>
 *   <li>{@code StaffProvisioningAdapter} — write-only owner creation (Tenant port)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    /** Roles that can be assigned via the Staff CRUD API. OWNER is restricted to tenant onboarding. */
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("ADMIN", "STAFF");

    // ── Read Operations ─────────────────────────────────────────────────────

    /**
     * Lists all staff members within the caller's tenant.
     *
     * <p>The tenant scope is automatically derived from the authenticated
     * {@code StaffPrincipal} — no client input is trusted for tenant filtering.</p>
     *
     * @return list of staff members in the current tenant
     */
    @Transactional(readOnly = true)
    public List<StaffResponse> listStaffForCurrentTenant() {
        UUID tenantId = SecurityUtil.getCurrentStaffPrincipal().tenantId();

        return staffRepository.findAllByTenantId(tenantId)
                .stream()
                .map(StaffResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single staff member by ID, scoped to the caller's tenant.
     *
     * <p>Even if a valid UUID is provided, it will only match if the staff
     * member belongs to the same tenant as the authenticated caller.</p>
     *
     * @param staffId the staff member's UUID
     * @return the staff member's details
     * @throws EntityNotFoundException if the staff member is not found within the tenant
     */
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(UUID staffId) {
        UUID tenantId = SecurityUtil.getCurrentStaffPrincipal().tenantId();

        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Staff member not found with ID: " + staffId
                ));

        return StaffResponse.fromEntity(staff);
    }

    // ── Write Operations ────────────────────────────────────────────────────

    /**
     * Creates a new staff member within the caller's tenant.
     *
     * <p>This method enforces several business rules:</p>
     * <ul>
     *   <li><b>Role validation:</b> Only ADMIN and STAFF roles can be assigned</li>
     *   <li><b>Branch validation:</b> The branch must belong to the same tenant</li>
     *   <li><b>Email uniqueness:</b> Globally unique across all tenants</li>
     *   <li><b>Password hashing:</b> Plaintext password is BCrypt-hashed before persistence</li>
     * </ul>
     *
     * @param request the validated creation data
     * @return the created staff member's details
     * @throws IllegalArgumentException if the role is invalid, email is taken, or branch doesn't belong to the tenant
     */
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        UUID tenantId = SecurityUtil.getCurrentStaffPrincipal().tenantId();

        // Step 1: Validate the requested role
        validateAssignableRole(request.role());

        // Step 2: Validate that the branch belongs to the current tenant
        validateBranchBelongsToTenant(request.branchId(), tenantId);

        // Step 3: Ensure global email uniqueness
        if (staffRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "Staff email '" + request.email() + "' is already registered."
            );
        }

        // Step 4: Hash the plaintext password
        String hashedPassword = passwordEncoder.encode(request.password());

        // Step 5: Create proxy entities to avoid unnecessary database lookups
        Tenant tenantProxy = new Tenant();
        tenantProxy.setId(tenantId);

        Branch branchProxy = new Branch();
        branchProxy.setId(request.branchId());

        // Step 6: Build and persist the Staff entity
        Staff staff = Staff.builder()
                .tenantId(tenantId)
                .tenant(tenantProxy)
                .branch(branchProxy)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .role(request.role().toUpperCase())
                .isActive(true)
                .passwordHash(hashedPassword)
                .build();

        staffRepository.save(staff);

        return StaffResponse.fromEntity(staff);
    }

    /**
     * Updates an existing staff member within the caller's tenant.
     *
     * <p><b>Immutable Fields:</b> Email and password cannot be changed through
     * this method. Email is an immutable login credential, and password changes
     * should be handled through a dedicated reset/change flow.</p>
     *
     * @param staffId the staff member's UUID
     * @param request the validated update data
     * @return the updated staff member's details
     * @throws EntityNotFoundException  if the staff member is not found within the tenant
     * @throws IllegalArgumentException if the requested role is invalid
     */
    @Transactional
    public StaffResponse updateStaff(UUID staffId, UpdateStaffRequest request) {
        UUID tenantId = SecurityUtil.getCurrentStaffPrincipal().tenantId();

        // Step 1: Find the staff member within the current tenant
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Staff member not found with ID: " + staffId
                ));

        // Step 2: Validate the requested role
        validateAssignableRole(request.role());

        // Step 3: Apply the updates
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        staff.setPhone(request.phone());
        staff.setRole(request.role().toUpperCase());
        staff.setIsActive(request.isActive());

        staffRepository.save(staff);

        return StaffResponse.fromEntity(staff);
    }

    /**
     * Deactivates a staff member within the caller's tenant.
     *
     * <p>This performs a <b>soft delete</b> — the staff record is retained in
     * the database but marked as inactive. Inactive accounts are rejected
     * during authentication by the {@code AuthenticationService}.</p>
     *
     * @param staffId the staff member's UUID
     * @throws EntityNotFoundException if the staff member is not found within the tenant
     */
    @Transactional
    public void deactivateStaff(UUID staffId) {
        UUID tenantId = SecurityUtil.getCurrentStaffPrincipal().tenantId();

        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Staff member not found with ID: " + staffId
                ));

        staff.setIsActive(false);
        staffRepository.save(staff);
    }

    // ── Validation Helpers ──────────────────────────────────────────────────

    /**
     * Ensures the requested role is one that can be assigned through the Staff CRUD API.
     * OWNER creation is restricted to the tenant onboarding flow, and SUPER_ADMIN
     * is a platform-level role that cannot be assigned here.
     */
    private void validateAssignableRole(String role) {
        if (!ASSIGNABLE_ROLES.contains(role.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid role '" + role + "'. Assignable roles are: " + ASSIGNABLE_ROLES
            );
        }
    }

    /**
     * Validates that the given branch belongs to the specified tenant.
     * Prevents a staff member from being assigned to a branch owned by another tenant.
     */
    private void validateBranchBelongsToTenant(UUID branchId, UUID tenantId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Branch not found with ID: " + branchId
                ));

        if (!branch.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException(
                    "Branch '" + branchId + "' does not belong to the current tenant."
            );
        }
    }
}
