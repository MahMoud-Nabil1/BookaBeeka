package com.system.booking.modules.hoteladmin.internal.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HotelAdminProfileResponse(
        UUID id,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Boolean isActive,
        LocalDateTime createdAt
) {}