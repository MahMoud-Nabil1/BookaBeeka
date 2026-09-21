package com.system.booking.modules.hoteladmin.internal.service;

import com.system.booking.modules.hoteladmin.internal.dto.HotelAdminProfileResponse;
import com.system.booking.modules.hoteladmin.internal.dto.UpdateHotelAdminProfileRequest;
import com.system.booking.modules.hoteladmin.internal.entity.HotelAdmin;
import com.system.booking.modules.hoteladmin.internal.repository.HotelAdminRepository;
import com.system.booking.modules.hoteladmin.port.in.HotelAdminProvisioningPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HotelAdminService implements HotelAdminProvisioningPort {

    private final HotelAdminRepository hotelAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public HotelAdminProfileResponse getProfile(UUID adminId, UUID tenantId) {
        HotelAdmin admin = hotelAdminRepository.findByIdAndTenantId(adminId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Hotel admin not found"));

        return toProfileResponse(admin);
    }

    @Transactional
    public HotelAdminProfileResponse updateProfile(UUID adminId, UUID tenantId, UpdateHotelAdminProfileRequest request) {
        HotelAdmin admin = hotelAdminRepository.findByIdAndTenantId(adminId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Hotel admin not found"));

        admin.setFirstName(request.firstName());
        admin.setLastName(request.lastName());
        admin.setPhone(request.phone());

        return toProfileResponse(hotelAdminRepository.save(admin));
    }

    @Override
    @Transactional
    public UUID createAdmin(
            UUID tenantId,
            String firstName,
            String lastName,
            String email,
            String plainPassword,
            String phone
    ) {
        if (hotelAdminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered");
        }

        HotelAdmin admin = HotelAdmin.builder()
                .tenantId(tenantId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordEncoder.encode(plainPassword))
                .phone(phone)
                .isActive(true)
                .build();

        return hotelAdminRepository.save(admin).getId();
    }

    private HotelAdminProfileResponse toProfileResponse(HotelAdmin admin) {
        return new HotelAdminProfileResponse(
                admin.getId(),
                admin.getTenantId(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getEmail(),
                admin.getPhone(),
                admin.getIsActive(),
                admin.getCreatedAt()
        );
    }
    @Override
    @Transactional(readOnly = true)
    public long countAdminsByTenantId(UUID tenantId) {
        return hotelAdminRepository.countByTenantId(tenantId);
    }
}