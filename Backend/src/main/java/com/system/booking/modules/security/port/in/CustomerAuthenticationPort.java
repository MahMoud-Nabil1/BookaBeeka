package com.system.booking.modules.security.port.in;

import com.system.booking.modules.security.dto.CustomerAuthDTO;
import java.util.Optional;

public interface CustomerAuthenticationPort {
    Optional<CustomerAuthDTO> findCustomerByEmail(String email);
}