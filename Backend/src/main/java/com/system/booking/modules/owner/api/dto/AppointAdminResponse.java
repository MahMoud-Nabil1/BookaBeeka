package com.system.booking.modules.owner.api.dto;

import java.util.UUID;

/**
 * Response returned after successfully appointing a new Admin.
 */
public record AppointAdminResponse(
        UUID staffId,
        String fullName,
        String email,
        String role,
        UUID branchId,
        String branchName,
        String message
) {}
