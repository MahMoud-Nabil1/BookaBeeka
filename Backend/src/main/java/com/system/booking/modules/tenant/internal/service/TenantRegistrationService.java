package com.system.booking.modules.tenant.internal.service;

import com.system.booking.modules.tenant.dto.request.CreateTenantRequest;
import com.system.booking.modules.tenant.dto.response.TenantRegistrationResponse;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.BranchRepository;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import com.system.booking.modules.tenant.port.out.StaffProvisioningPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating the atomic tenant onboarding process.
 *
 * <h3>Onboarding Flow (Single Transaction):</h3>
 * <ol>
 *   <li><b>Validate</b> — Check that the subdomain is unique</li>
 *   <li><b>Create Tenant</b> — Persist the business entity with ACTIVE status</li>
 *   <li><b>Create Default Branch</b> — Persist the main branch linked to the tenant</li>
 *   <li><b>Create Owner</b> — Delegate to {@link StaffProvisioningPort} to create
 *       the OWNER staff member with hashed password in the Staff module</li>
 * </ol>
 *
 * <p><b>Atomicity Guarantee:</b> The entire flow runs within a single {@code @Transactional}
 * boundary. If any step fails (e.g., duplicate subdomain, email already exists, database
 * error), the entire operation rolls back — no partial tenant state is left behind.</p>
 *
 * <p><b>Hexagonal Pattern:</b> This service does not directly create Staff entities.
 * It delegates to the {@code StaffProvisioningPort} (implemented by the Staff module's
 * {@code StaffProvisioningAdapter}), maintaining strict module boundaries.</p>
 */
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final StaffProvisioningPort staffProvisioningPort;

    /**
     * Registers a new tenant with a default branch and owner staff member.
     *
     * @param request the onboarding data (tenant info + branch info + owner credentials)
     * @return a response containing the IDs of all created entities
     * @throws IllegalArgumentException if the subdomain is already taken
     */
    @Transactional
    public TenantRegistrationResponse registerTenant(CreateTenantRequest request) {

        // ── Step 1: Validate subdomain uniqueness ───────────────────────────
        if (tenantRepository.existsBySubdomain(request.subdomain())) {
            throw new IllegalArgumentException(
                    "Subdomain '" + request.subdomain() + "' is already taken."
            );
        }

        // ── Step 2: Create the Tenant entity ────────────────────────────────
        Tenant tenant = Tenant.builder()
                .name(request.tenantName())
                .subdomain(request.subdomain())
                .status("ACTIVE")
                .build();
        tenantRepository.save(tenant);

        // ── Step 3: Create the Default Branch ───────────────────────────────
        Branch branch = Branch.builder()
                .tenantId(tenant.getId())   // Links the branch to the tenant
                .name(request.branchName())
                .address(request.branchAddress())
                .status("ACTIVE")
                .build();
        branchRepository.save(branch);

        // ── Step 4: Delegate Owner creation to Staff module via port ────────
        // The StaffProvisioningAdapter handles password hashing and entity persistence.
        // This call participates in the same transaction — if it fails, everything rolls back.
        staffProvisioningPort.createOwnerStaff(
                tenant.getId(),
                branch.getId(),
                request.ownerFirstName(),
                request.ownerLastName(),
                request.ownerEmail(),
                request.ownerPassword(),  // Plaintext — hashed by the adapter
                request.ownerPhone()
        );

        // ── Step 5: Return confirmation with all created IDs ────────────────
        return new TenantRegistrationResponse(
                tenant.getId(),
                branch.getId(),
                request.ownerEmail(),
                "Tenant '" + request.tenantName() + "' registered successfully."
        );
    }
}
