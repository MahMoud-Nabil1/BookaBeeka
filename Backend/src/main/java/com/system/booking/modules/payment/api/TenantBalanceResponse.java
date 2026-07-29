package com.system.booking.modules.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Snapshot of a tenant's current revenue balance.
 *
 * @param tenantWalletId unique ID of the TenantWallet record
 * @param tenantId       UUID of the tenant
 * @param balance        current accumulated revenue balance
 * @param currency       ISO-4217 currency code
 * @param updatedAt      timestamp of the last balance change
 */
public record TenantBalanceResponse(
        UUID          tenantWalletId,
        UUID          tenantId,
        BigDecimal    balance,
        String        currency,
        LocalDateTime updatedAt
) {}
