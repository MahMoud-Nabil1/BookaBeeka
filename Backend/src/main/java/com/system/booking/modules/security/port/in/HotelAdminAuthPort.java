package com.system.booking.modules.security.port.in;
import com.system.booking.modules.security.dto.AuthUserDTO;
import java.util.Optional;
import java.util.UUID;

public interface HotelAdminAuthPort {
    Optional<AuthUserDTO> findAdminByEmail(String email);
    void updatePassword(UUID adminId, String newPasswordHash);
}