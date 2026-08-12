package com.system.booking.modules.security.dto;

import java.util.UUID;

public record CustomerAuthDTO(
        UUID id,
        String email,
        String passwordHash
) {}