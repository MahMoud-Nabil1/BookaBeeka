package com.system.booking.modules.staff.internal.service;

import com.system.booking.modules.security.dto.StaffAuthDTO;
import com.system.booking.modules.security.port.in.StaffAuthenticationPort;
import com.system.booking.modules.staff.internal.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter implementing the {@link StaffAuthenticationPort} (defined in the Security module).
 *
 * <p><b>Responsibility:</b> Strictly handles <b>read-only</b> operations for Staff
 * authentication. This ensures the Security module can retrieve staff credentials
 * without being coupled to the internal {@code StaffRepository}.</p>
 *
 * <p><b>Separation of Concerns:</b> Write operations (such as creating new staff members)
 * are deliberately excluded from this adapter and are handled by the
 * {@code StaffProvisioningAdapter} instead.</p>
 */
@Service
@RequiredArgsConstructor
public class StaffSecurityAdapter implements StaffAuthenticationPort {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffAuthDTO> findStaffByEmail(String email) {
        // Find the staff entity and map it to the DTO expected by the Security module
        return staffRepository.findByEmail(email)
                .map(staff -> new StaffAuthDTO(
                        staff.getId(), // Inherited from BaseEntity
                        staff.getEmail(),
                        staff.getPasswordHash(),
                        staff.getRole(),
                        staff.getTenantId(), // Inherited from TenantBaseEntity
                        staff.getBranch().getId(),
                        staff.getFirstName(),
                        staff.getLastName(),
                        staff.getIsActive()
                ));
    }
}
