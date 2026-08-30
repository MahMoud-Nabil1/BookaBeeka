package com.system.booking.modules.admin.api.dto;

public record SuperAdminLoginResponse(
        String token,
        String userType   // always "SUPER_ADMIN"
) {}
