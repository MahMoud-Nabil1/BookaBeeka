package com.system.booking.modules.customer.internal.security;

import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.port.in.CustomerAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerSecurityAdapter implements CustomerAuthPort {
    private final CustomerRepository repository;

    @Override
    public Optional<AuthUserDTO> findCustomerByEmail(String email) {
        return repository.findByEmail(email)
                .map(c -> new AuthUserDTO(c.getId(), c.getEmail(), c.getPasswordHash(), "CUSTOMER", null, c.getIsActive()));
    }

    @Override
    @Transactional
    public void updatePassword(UUID customerId, String newPasswordHash) {
        repository.findById(customerId).ifPresent(c -> {
            c.setPasswordHash(newPasswordHash);
            repository.save(c);
        });
    }
}