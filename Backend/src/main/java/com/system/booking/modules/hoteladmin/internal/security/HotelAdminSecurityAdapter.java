package com.system.booking.modules.hoteladmin.internal.security;

import com.system.booking.modules.hoteladmin.internal.repository.HotelAdminRepository;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.port.in.HotelAdminAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
}