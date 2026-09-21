package com.system.booking.modules.owner.internal.dto;

import java.util.UUID;

public record AppointAdminResponse(
        UUID adminId,
        String fullName,
        String email,
        String role,
        String message
) {}