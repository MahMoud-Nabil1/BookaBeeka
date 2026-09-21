package com.system.booking.modules.superAdmin.internal.security;

import com.system.booking.modules.superAdmin.internal.repository.SuperAdminRepository;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.port.in.SuperAdminAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SuperAdminSecurityAdapter implements SuperAdminAuthPort {
    private final SuperAdminRepository repository;

    @Override
    public Optional<AuthUserDTO> findSuperAdminByEmail(String email) {
        return repository.findByEmail(email)
                .map(a -> new AuthUserDTO(a.getId(), a.getEmail(), a.getPasswordHash(), "SUPER_ADMIN", null, a.getIsActive()));
    }

    @Override
    @Transactional
    public void updatePassword(UUID superAdminId, String newPasswordHash) {
        repository.findById(superAdminId).ifPresent(admin -> {
            admin.setPasswordHash(newPasswordHash);
            repository.save(admin);
        });
    }
}