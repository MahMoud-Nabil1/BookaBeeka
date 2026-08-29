package com.system.booking.modules.owner.internal.service;

import com.system.booking.modules.owner.api.dto.*;
import com.system.booking.modules.owner.internal.repository.OwnerReportingRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.staff.internal.entity.Staff;
import com.system.booking.modules.staff.internal.repository.StaffRepository;
import com.system.booking.modules.tenant.api.dto.BranchDto;
import com.system.booking.modules.tenant.api.dto.UpdateBranchRequest;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.BranchRepository;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import com.system.booking.modules.tenant.internal.service.BranchService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Core business service for Owner-level operations.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li><b>Admin Management</b> — Appoint/create and list Admins within the Owner's tenant</li>
 *   <li><b>Tenant Provisioning</b> — Create new tenants on behalf of the Owner</li>
 *   <li><b>Dashboard & KPIs</b> — Aggregate revenue, branch statuses, and booking metrics</li>
 *   <li><b>Revenue Reporting</b> — Compute overall and branch-by-branch revenue breakdowns</li>
 *   <li><b>Branch Editing</b> — Delegate branch updates to {@link BranchService}</li>
 * </ul>
 *
 * <p><b>Tenant Isolation:</b> All methods receive the {@code tenantId} from the
 * authenticated {@code StaffPrincipal} and use it to scope every database operation.</p>
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerReportingRepository reportingRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final TenantWalletRepository tenantWalletRepository;
    private final BranchService branchService;
    private final PasswordEncoder passwordEncoder;

    // ── Admin Management ────────────────────────────────────────────────────

    /**
     * Appoints a new Admin for a branch within the Owner's tenant.
     *
     * <p>Validates branch ownership, email uniqueness, hashes the password,
     * and persists the staff member with {@code role = "ADMIN"}.</p>
     *
     * @param tenantId the Owner's tenant UUID (from JWT)
     * @param request  the Admin's profile and credentials
     * @return confirmation with the new Admin's details
     * @throws IllegalArgumentException  if the branch doesn't belong to the tenant or email is taken
     * @throws EntityNotFoundException   if the branch does not exist
     */
    @Transactional
    public AppointAdminResponse appointAdmin(UUID tenantId, AppointAdminRequest request) {

        // Step 1: Validate branch belongs to the Owner's tenant
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Branch not found with ID: " + request.branchId()
                ));

        if (!branch.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException(
                    "Branch '" + request.branchId() + "' does not belong to the current tenant."
            );
        }

        // Step 2: Ensure global email uniqueness
        if (staffRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "Email '" + request.email() + "' is already registered."
            );
        }

        // Step 3: Hash the password
        String hashedPassword = passwordEncoder.encode(request.password());

        // Step 4: Build proxy entities (avoids unnecessary DB lookups)
        Tenant tenantProxy = new Tenant();
        tenantProxy.setId(tenantId);

        // Step 5: Build and persist the Admin staff entity
        Staff admin = Staff.builder()
                .tenantId(tenantId)
                .tenant(tenantProxy)
                .branch(branch)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .role("ADMIN")
                .isActive(true)
                .passwordHash(hashedPassword)
                .build();

        staffRepository.save(admin);

        return new AppointAdminResponse(
                admin.getId(),
                admin.getFirstName() + " " + admin.getLastName(),
                admin.getEmail(),
                admin.getRole(),
                branch.getId(),
                branch.getName(),
                "Admin '" + admin.getFirstName() + " " + admin.getLastName()
                        + "' appointed successfully to branch '" + branch.getName() + "'."
        );
    }

    /**
     * Lists all Admins within the Owner's tenant with full profile and branch details.
     *
     * @param tenantId the Owner's tenant UUID
     * @return list of Admin summaries
     */
    @Transactional(readOnly = true)
    public List<OwnerAdminSummaryDto> listAdmins(UUID tenantId) {
        return reportingRepository.listTenantAdmins(tenantId);
    }

    // ── Tenant Provisioning ─────────────────────────────────────────────────

    /**
     * Creates a new Tenant with a default Branch on behalf of the authenticated Owner.
     *
     * <p>The Owner is automatically provisioned as the OWNER staff member
     * in the newly created tenant using their existing profile credentials.</p>
     *
     * @param ownerStaffId the authenticated Owner's staff UUID
     * @param tenantId     the Owner's current tenant UUID (for retrieving their profile)
     * @param request      the new tenant and branch details
     * @return confirmation with the created tenant and branch IDs
     * @throws IllegalArgumentException if the subdomain is already taken
     * @throws EntityNotFoundException  if the Owner's staff record is not found
     */
    @Transactional
    public CreateOwnerTenantResponse createNewTenant(
            UUID ownerStaffId,
            UUID tenantId,
            CreateOwnerTenantRequest request
    ) {
        // Step 1: Validate subdomain uniqueness
        if (tenantRepository.existsBySubdomain(request.subdomain())) {
            throw new IllegalArgumentException(
                    "Subdomain '" + request.subdomain() + "' is already taken."
            );
        }

        // Step 2: Retrieve the Owner's existing staff profile
        Staff ownerStaff = staffRepository.findByIdAndTenantId(ownerStaffId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Owner staff record not found."
                ));

        // Step 3: Create the Tenant entity
        Tenant newTenant = Tenant.builder()
                .name(request.tenantName())
                .subdomain(request.subdomain())
                .status("ACTIVE")
                .timezone(request.timezone() != null ? request.timezone() : "UTC")
                .currency(request.currency() != null ? request.currency() : "USD")
                .build();
        tenantRepository.save(newTenant);

        // Step 4: Create the default Branch
        Branch defaultBranch = Branch.builder()
                .tenantId(newTenant.getId())
                .name(request.branchName())
                .address(request.branchAddress())
                .status("ACTIVE")
                .build();
        branchRepository.save(defaultBranch);

        // Step 5: Provision the Owner as OWNER staff in the new tenant
        Tenant newTenantProxy = new Tenant();
        newTenantProxy.setId(newTenant.getId());

        Staff ownerInNewTenant = Staff.builder()
                .tenantId(newTenant.getId())
                .tenant(newTenantProxy)
                .branch(defaultBranch)
                .firstName(ownerStaff.getFirstName())
                .lastName(ownerStaff.getLastName())
                .email(ownerStaff.getEmail() + "+" + request.subdomain()) // unique email per tenant
                .phone(ownerStaff.getPhone())
                .role("OWNER")
                .isActive(true)
                .passwordHash(ownerStaff.getPasswordHash()) // reuse existing hashed password
                .build();
        staffRepository.save(ownerInNewTenant);

        return new CreateOwnerTenantResponse(
                newTenant.getId(),
                newTenant.getName(),
                newTenant.getSubdomain(),
                defaultBranch.getId(),
                "Tenant '" + request.tenantName() + "' created successfully."
        );
    }

    // ── Dashboard & KPIs ────────────────────────────────────────────────────

    /**
     * Builds the Owner's main dashboard with tenant summary, branch overviews,
     * overall revenue, wallet balance, and high-level KPIs.
     *
     * @param tenantId the Owner's tenant UUID
     * @return aggregated dashboard data
     */
    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        BigDecimal overallRevenue = reportingRepository.getOverallTenantRevenue(tenantId);
        long totalBranches = reportingRepository.countBranches(tenantId);
        long activeBranches = reportingRepository.countActiveBranches(tenantId);
        long totalBookings = reportingRepository.getTotalBookingCount(tenantId);
        long totalAdmins = reportingRepository.countAdmins(tenantId);
        List<OwnerBranchSummaryDto> branches = reportingRepository.getBranchSummaries(tenantId);

        BigDecimal walletBalance = tenantWalletRepository.findByTenantId(tenantId)
                .map(w -> w.getBalance())
                .orElse(BigDecimal.ZERO);

        OwnerTenantSummaryDto tenantSummary = new OwnerTenantSummaryDto(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getStatus(),
                tenant.getTimezone(),
                tenant.getCurrency(),
                tenant.getCreatedAt(),
                totalBranches,
                overallRevenue
        );

        return new OwnerDashboardResponse(
                tenantSummary,
                overallRevenue,
                walletBalance,
                tenant.getCurrency() != null ? tenant.getCurrency() : "USD",
                totalBranches,
                activeBranches,
                totalBookings,
                totalAdmins,
                branches
        );
    }

    // ── Tenant Listing ──────────────────────────────────────────────────────

    /**
     * Returns the Owner's tenant summary (currently single-tenant per Owner).
     *
     * @param tenantId the Owner's tenant UUID
     * @return list containing the Owner's tenant summary
     */
    @Transactional(readOnly = true)
    public List<OwnerTenantSummaryDto> getOwnedTenants(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        BigDecimal revenue = reportingRepository.getOverallTenantRevenue(tenantId);
        long branchCount = reportingRepository.countBranches(tenantId);

        OwnerTenantSummaryDto summary = new OwnerTenantSummaryDto(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getStatus(),
                tenant.getTimezone(),
                tenant.getCurrency(),
                tenant.getCreatedAt(),
                branchCount,
                revenue
        );

        return List.of(summary);
    }

    // ── Branch Operations ───────────────────────────────────────────────────

    /**
     * Lists all branches with status, staff count, and revenue metrics.
     *
     * @param tenantId the Owner's tenant UUID
     * @return list of branch summaries
     */
    @Transactional(readOnly = true)
    public List<OwnerBranchSummaryDto> getBranchesWithStatusAndRevenue(UUID tenantId) {
        return reportingRepository.getBranchSummaries(tenantId);
    }

    /**
     * Edits a branch's details by delegating to {@link BranchService}.
     *
     * @param tenantId  the Owner's tenant UUID
     * @param branchId  the branch to edit
     * @param request   the fields to update
     * @return the updated branch DTO
     */
    @Transactional
    public BranchDto editBranch(UUID tenantId, UUID branchId, EditBranchRequest request) {
        UpdateBranchRequest updateRequest = new UpdateBranchRequest(
                request.name(),
                request.address(),
                request.status(),
                request.settings()
        );
        return branchService.updateBranch(tenantId, branchId, updateRequest);
    }

    // ── Revenue Reporting ───────────────────────────────────────────────────

    /**
     * Computes a deep-dive revenue report with overall tenant revenue
     * and branch-by-branch financial breakdown.
     *
     * @param tenantId the Owner's tenant UUID
     * @return revenue summary with branch breakdown
     */
    @Transactional(readOnly = true)
    public OwnerRevenueSummaryDto getRevenueSummary(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        BigDecimal overallRevenue = reportingRepository.getOverallTenantRevenue(tenantId);
        long completedBookings = reportingRepository.getCompletedBookingCount(tenantId);
        List<BranchRevenueBreakdownDto> breakdown = reportingRepository.getBranchRevenueBreakdown(tenantId);

        BigDecimal walletBalance = tenantWalletRepository.findByTenantId(tenantId)
                .map(w -> w.getBalance())
                .orElse(BigDecimal.ZERO);

        return new OwnerRevenueSummaryDto(
                tenant.getId(),
                tenant.getName(),
                overallRevenue,
                walletBalance,
                tenant.getCurrency() != null ? tenant.getCurrency() : "USD",
                completedBookings,
                breakdown
        );
    }
}
