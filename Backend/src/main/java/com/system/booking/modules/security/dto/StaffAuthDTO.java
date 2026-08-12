package com.system.booking.modules.security.dto;

import java.util.UUID;

public record StaffAuthDTO(
        UUID id,
        String email,
        String passwordHash,
        String role,
        UUID tenantId,
        UUID branchId,
        boolean isActive
) {}