package com.system.booking.modules.security.port.in;
import com.system.booking.modules.security.dto.AuthUserDTO;
import java.util.Optional;
public interface CustomerAuthPort {
    Optional<AuthUserDTO> findCustomerByEmail(String email);
}