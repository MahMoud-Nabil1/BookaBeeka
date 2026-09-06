package com.system.booking.modules.security.port.in;
import com.system.booking.modules.security.dto.AuthUserDTO;
import java.util.Optional;
public interface HotelAdminAuthPort {
    Optional<AuthUserDTO> findAdminByEmail(String email);
}