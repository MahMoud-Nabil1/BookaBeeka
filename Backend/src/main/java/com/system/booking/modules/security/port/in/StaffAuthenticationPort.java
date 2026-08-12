package com.system.booking.modules.security.port.in;

import com.system.booking.modules.security.dto.StaffAuthDTO;
import java.util.Optional;

public interface StaffAuthenticationPort {
    Optional<StaffAuthDTO> findStaffByEmail(String email);
}