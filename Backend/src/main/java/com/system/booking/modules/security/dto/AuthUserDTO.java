package com.system.booking.modules.security.dto;

import java.util.UUID;

public record AuthUserDTO(
        UUID id,
        String email,
        String passwordHash,
        String role,
        UUID tenantId, // null للـ SuperAdmin والـ Customer
        boolean isActive
) {}