package com.system.booking.modules.security.port.in;
import com.system.booking.modules.security.dto.AuthUserDTO;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAuthPort {
    Optional<AuthUserDTO> findCustomerByEmail(String email);
    void updatePassword(UUID customerId, String newPasswordHash);
}