package com.system.booking.modules.staff.internal.service;

import com.system.booking.modules.staff.internal.entity.Staff;
import com.system.booking.modules.staff.internal.repository.StaffRepository;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.port.out.StaffProvisioningPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Adapter implementing the {@link StaffProvisioningPort} (defined in the Tenant module).
 *
 * <p><b>Responsibility:</b> Strictly handles <b>write-only</b> operations for staff
 * creation. This adapter is called by the {@code TenantRegistrationService} during
 * the atomic tenant onboarding flow.</p>
 *
 * <p><b>Security Guarantee:</b> This adapter is responsible for hashing the plaintext
 * password via {@link PasswordEncoder} before persisting the entity. The Tenant
 * module passes the raw password but never touches the hashing logic itself.</p>
 */
@Service
@RequiredArgsConstructor
public class StaffProvisioningAdapter implements StaffProvisioningPort {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates an OWNER staff member.
     *
     * <p>Runs within the existing transaction initiated by {@code TenantRegistrationService}.
     * If this method throws an exception (e.g., duplicate email), the entire tenant
     * registration rolls back.</p>
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY) // Must run within an existing transaction
    public void createOwnerStaff(
            UUID tenantId,
            UUID branchId,
            String firstName,
            String lastName,
            String email,
            String password,
            String phone
    ) {
        // Step 1: Prevent duplicate emails across the entire system
        if (staffRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Staff email '" + email + "' is already registered.");
        }

        // Step 2: Hash the plaintext password securely
        String hashedPassword = passwordEncoder.encode(password);

        // We use proxy entities to avoid hitting the database for lookups since we only need the IDs
        Tenant tenantProxy = new Tenant();
        tenantProxy.setId(tenantId);

        Branch branchProxy = new Branch();
        branchProxy.setId(branchId);

        // Step 3: Build and save the Staff entity
        Staff owner = Staff.builder()
                .tenantId(tenantId)
                .tenant(tenantProxy)
                .branch(branchProxy)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .role("OWNER") // Assign the highest tenant-level role
                .isActive(true)
                .passwordHash(hashedPassword)
                .build();

        staffRepository.save(owner);
    }
}
