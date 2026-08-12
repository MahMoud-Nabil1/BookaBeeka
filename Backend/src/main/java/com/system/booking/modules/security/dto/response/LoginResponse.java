package com.system.booking.modules.security.dto.response;

public record LoginResponse(
        String token,
        String userType
) {}