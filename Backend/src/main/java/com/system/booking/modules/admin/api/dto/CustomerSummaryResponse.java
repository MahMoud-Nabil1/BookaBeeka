package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Row in the SuperAdmin customer management table. */
public record CustomerSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Boolean isActive,
        BigDecimal walletBalance,   // null if no wallet yet
        LocalDateTime createdAt
) {}
