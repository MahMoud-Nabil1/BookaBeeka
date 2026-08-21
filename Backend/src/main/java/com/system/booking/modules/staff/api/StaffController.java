package com.system.booking.modules.staff.api;

import com.system.booking.modules.staff.internal.dto.request.CreateStaffRequest;
import com.system.booking.modules.staff.internal.dto.request.UpdateStaffRequest;
import com.system.booking.modules.staff.internal.dto.response.StaffResponse;
import com.system.booking.modules.staff.internal.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for tenant-scoped staff management operations.
 *
 * <p>Exposes CRUD endpoints for managing staff members within the caller's
 * tenant. All endpoints are secured with method-level authorization using
 * {@code @PreAuthorize} and automatically scope data to the authenticated
 * staff member's tenant via {@code SecurityUtil}.</p>
 *
 * <h3>Endpoint Summary:</h3>
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Access</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/api/staff</td><td>OWNER, ADMIN</td><td>List all staff within current tenant</td></tr>
 *   <tr><td>GET</td><td>/api/staff/{id}</td><td>OWNER, ADMIN</td><td>Get staff details by ID</td></tr>
 *   <tr><td>POST</td><td>/api/staff</td><td>OWNER, ADMIN</td><td>Create a new staff member</td></tr>
 *   <tr><td>PUT</td><td>/api/staff/{id}</td><td>OWNER, ADMIN</td><td>Update staff details</td></tr>
 *   <tr><td>DELETE</td><td>/api/staff/{id}</td><td>OWNER</td><td>Deactivate (soft-delete) a staff member</td></tr>
 * </table>
 *
 * <p><b>Tenant Isolation:</b> Even though all endpoints are accessible by any
 * OWNER or ADMIN with a valid JWT, the underlying {@code StaffService} enforces
 * tenant scoping — a staff member can never see or modify data from another tenant.</p>
 *
 * <p><b>Note:</b> The DELETE endpoint performs a <b>soft delete</b> (deactivation),
 * not a hard delete. The staff record remains in the database but is marked as
 * inactive, preventing future authentication.</p>
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    /**
     * Lists all staff members within the caller's tenant.
     *
     * @return 200 OK with list of staff members
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<StaffResponse>> listStaff() {
        List<StaffResponse> staff = staffService.listStaffForCurrentTenant();
        return ResponseEntity.ok(staff);
    }

    /**
     * Retrieves a single staff member by ID within the caller's tenant.
     *
     * @param id the staff member's UUID
     * @return 200 OK with the staff member's details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<StaffResponse> getStaff(@PathVariable UUID id) {
        StaffResponse staff = staffService.getStaffById(id);
        return ResponseEntity.ok(staff);
    }

    /**
     * Creates a new staff member within the caller's tenant.
     *
     * <p>The staff member is automatically associated with the caller's
     * tenant. Only ADMIN and STAFF roles can be assigned.</p>
     *
     * @param request the validated creation data
     * @return 201 Created with the new staff member's details
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<StaffResponse> createStaff(
            @Valid @RequestBody CreateStaffRequest request
    ) {
        StaffResponse staff = staffService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(staff);
    }

    /**
     * Updates an existing staff member within the caller's tenant.
     *
     * <p>Email and password cannot be changed through this endpoint.</p>
     *
     * @param id      the staff member's UUID
     * @param request the validated update data
     * @return 200 OK with the updated staff member's details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        StaffResponse staff = staffService.updateStaff(id, request);
        return ResponseEntity.ok(staff);
    }

    /**
     * Deactivates (soft-deletes) a staff member within the caller's tenant.
     *
     * <p><b>Access:</b> OWNER only — deactivation is a high-impact operation
     * that should be restricted to the tenant owner.</p>
     *
     * @param id the staff member's UUID
     * @return 204 No Content on successful deactivation
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deactivateStaff(@PathVariable UUID id) {
        staffService.deactivateStaff(id);
        return ResponseEntity.noContent().build();
    }
}
