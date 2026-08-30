package com.system.booking.modules.owner.api;

import com.system.booking.modules.owner.api.dto.*;
import com.system.booking.modules.owner.internal.service.OwnerService;
import com.system.booking.modules.security.model.principal.StaffPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import com.system.booking.modules.tenant.api.dto.BranchDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Owner REST API — endpoints for tenant owners to manage their business.
 *
 * <p>Base path: {@code /api/v1/owner}</p>
 *
 * <p>All endpoints are secured with {@code @PreAuthorize("hasRole('OWNER')")} and
 * automatically scope data to the authenticated Owner's tenant via
 * {@link SecurityUtil#getCurrentStaffPrincipal()}.</p>
 *
 * <h3>Endpoint Summary:</h3>
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>POST</td><td>/admins</td><td>Appoint/create a new Admin</td></tr>
 *   <tr><td>GET</td><td>/admins</td><td>List all Admins with full details</td></tr>
 *   <tr><td>POST</td><td>/tenants</td><td>Create a new tenant</td></tr>
 *   <tr><td>GET</td><td>/dashboard</td><td>Owner dashboard with KPIs</td></tr>
 *   <tr><td>GET</td><td>/tenants</td><td>List owned tenants</td></tr>
 *   <tr><td>GET</td><td>/branches</td><td>List branches with status and revenue</td></tr>
 *   <tr><td>PATCH</td><td>/branches/{branchId}</td><td>Edit branch details</td></tr>
 *   <tr><td>GET</td><td>/revenue</td><td>Revenue breakdown</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerController {

    private final OwnerService ownerService;

    // ── Admin Management ────────────────────────────────────────────────────

    /**
     * Appoints/creates a new Admin for a branch under the Owner's tenant.
     *
     * @param request the Admin's profile and credentials
     * @return 201 Created with the new Admin's details
     */
    @PostMapping("/admins")
    public ResponseEntity<AppointAdminResponse> appointAdmin(
            @Valid @RequestBody AppointAdminRequest request) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        AppointAdminResponse response = ownerService.appointAdmin(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all Admins within the Owner's tenant with full profile
     * and branch details.
     *
     * @return 200 OK with list of Admin summaries
     */
    @GetMapping("/admins")
    public ResponseEntity<List<OwnerAdminSummaryDto>> listAdmins() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return ResponseEntity.ok(ownerService.listAdmins(principal.tenantId()));
    }

    // ── Tenant Provisioning ─────────────────────────────────────────────────

    /**
     * Creates a new Tenant with a default Branch on behalf of the Owner.
     *
     * @param request the new tenant and branch details
     * @return 201 Created with the new tenant and branch IDs
     */
    @PostMapping("/tenants")
    public ResponseEntity<CreateOwnerTenantResponse> createTenant(
            @Valid @RequestBody CreateOwnerTenantRequest request) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        CreateOwnerTenantResponse response = ownerService.createNewTenant(
                principal.id(), principal.tenantId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Dashboard ───────────────────────────────────────────────────────────

    /**
     * Returns the Owner's main dashboard with tenant summary, branch overviews,
     * overall revenue, wallet balance, and high-level KPIs.
     *
     * @return 200 OK with dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<OwnerDashboardResponse> getDashboard() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return ResponseEntity.ok(ownerService.getDashboard(principal.tenantId()));
    }

    // ── Tenant Listing ──────────────────────────────────────────────────────

    /**
     * Lists the Owner's tenant(s) with status, branch counts, and revenue.
     *
     * @return 200 OK with list of tenant summaries
     */
    @GetMapping("/tenants")
    public ResponseEntity<List<OwnerTenantSummaryDto>> listTenants() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return ResponseEntity.ok(ownerService.getOwnedTenants(principal.tenantId()));
    }

    // ── Branch Operations ───────────────────────────────────────────────────

    /**
     * Lists all branches with live status, staff counts, and revenue metrics.
     *
     * @return 200 OK with list of branch summaries
     */
    @GetMapping("/branches")
    public ResponseEntity<List<OwnerBranchSummaryDto>> listBranches() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return ResponseEntity.ok(ownerService.getBranchesWithStatusAndRevenue(principal.tenantId()));
    }

    /**
     * Edits a branch's details (name, address, status, settings).
     *
     * @param branchId the branch UUID
     * @param request  the fields to update (all optional)
     * @return 200 OK with the updated branch details
     */
    @PatchMapping("/branches/{branchId}")
    public ResponseEntity<BranchDto> editBranch(
            @PathVariable UUID branchId,
            @RequestBody EditBranchRequest request) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return ResponseEntity.ok(ownerService.editBranch(principal.tenantId(), branchId, request));
    }

    // ── Revenue ─────────────────────────────────────────────────────────────

    /**
     * Returns the Owner's revenue summary with overall tenant revenue
     * and branch-by-branch financial breakdown.
     *
     * @return 200 OK with revenue summary
     */
    @GetMapping("/revenue")
    public ResponseEntity<OwnerRevenueSummaryDto> getRevenueSummary() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return ResponseEntity.ok(ownerService.getRevenueSummary(principal.tenantId()));
    }
}
