package com.system.booking.modules.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Row in the SuperAdmin customer wallet overview table.
 * Returned by {@code GET /api/admin/super/wallets/customers}.
 */
public record CustomerWalletSummaryResponse(
        UUID walletId,
        UUID customerId,
        String customerEmail,
        BigDecimal balance,
        String currency,
        LocalDateTime updatedAt
) {}
