package com.system.booking.modules.staff.internal.service;

import com.system.booking.modules.security.dto.StaffAuthDTO;
import com.system.booking.modules.security.port.in.StaffAuthenticationPort;
import com.system.booking.modules.staff.internal.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffSecurityAdapter implements StaffAuthenticationPort {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffAuthDTO> findStaffByEmail(String email) {
        // 1. بندور على الموظف في الداتا بيز
        return staffRepository.findByEmail(email)
                .map(staff -> new StaffAuthDTO(
                        staff.getId(), // موروث من BaseEntity
                        staff.getEmail(),
                        staff.getPasswordHash(),
                        staff.getRole(),
                        staff.getTenantId(), // موروث من TenantBaseEntity
                        staff.getBranch().getId(), // بنجيب الـ ID بتاع الفرع
                        staff.getIsActive()
                ));
    }
}
