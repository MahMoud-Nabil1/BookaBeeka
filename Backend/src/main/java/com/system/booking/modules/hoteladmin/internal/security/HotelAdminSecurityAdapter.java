package com.system.booking.modules.hoteladmin.internal.security;

import com.system.booking.modules.hoteladmin.internal.repository.HotelAdminRepository;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.port.in.HotelAdminAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HotelAdminSecurityAdapter implements HotelAdminAuthPort {

    private final HotelAdminRepository hotelAdminRepository;

    @Override
    public Optional<AuthUserDTO> findAdminByEmail(String email) {
        return hotelAdminRepository.findByEmail(email)
                .map(admin -> new AuthUserDTO(
                        admin.getId(),
                        admin.getEmail(),
                        admin.getPasswordHash(),
                        "ADMIN",
                        admin.getTenantId(),
                        admin.getIsActive()
                ));
    }

    @Override
    @Transactional
    public void updatePassword(UUID adminId, String newPasswordHash) {
        hotelAdminRepository.findById(adminId).ifPresent(admin -> {
            admin.setPasswordHash(newPasswordHash);
            hotelAdminRepository.save(admin);
        });
    }
}