package com.system.booking.modules.owner.internal.security;

import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.port.in.OwnerAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OwnerSecurityAdapter implements OwnerAuthPort {

    private final OwnerRepository ownerRepository;

    @Override
    public Optional<AuthUserDTO> findOwnerByEmail(String email) {
        return ownerRepository.findByEmail(email)
                .map(owner -> new AuthUserDTO(
                        owner.getId(),
                        owner.getEmail(),
                        owner.getPasswordHash(),
                        "OWNER",
                        owner.getTenantId(),
                        owner.getIsActive()
                ));
    }
}