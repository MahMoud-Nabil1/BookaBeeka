package com.system.booking.modules.security.port.in;
import com.system.booking.modules.security.dto.AuthUserDTO;
import java.util.Optional;
public interface OwnerAuthPort {
    Optional<AuthUserDTO> findOwnerByEmail(String email);
}