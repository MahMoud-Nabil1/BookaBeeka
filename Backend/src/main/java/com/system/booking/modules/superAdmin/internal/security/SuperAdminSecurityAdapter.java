package com.system.booking.modules.superAdmin.internal.security;

import com.system.booking.modules.superAdmin.internal.repository.SuperAdminRepository;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.port.in.SuperAdminAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SuperAdminSecurityAdapter implements SuperAdminAuthPort {
    private final SuperAdminRepository repository;

    @Override
    public Optional<AuthUserDTO> findSuperAdminByEmail(String email) {
        return repository.findByEmail(email)
                .map(a -> new AuthUserDTO(a.getId(), a.getEmail(), a.getPasswordHash(), "SUPER_ADMIN", null, a.getIsActive()));
    }
}