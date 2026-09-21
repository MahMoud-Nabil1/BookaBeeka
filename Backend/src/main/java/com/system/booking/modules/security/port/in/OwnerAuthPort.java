package com.system.booking.modules.security.port.in;
import com.system.booking.modules.security.dto.AuthUserDTO;
import java.util.Optional;
import java.util.UUID;

public interface OwnerAuthPort {
    Optional<AuthUserDTO> findOwnerByEmail(String email);
    void updatePassword(UUID ownerId, String newPasswordHash);
}