package com.system.booking.modules.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SuperAdminLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
