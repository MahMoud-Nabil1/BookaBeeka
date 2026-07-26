package com.system.booking.modules.availability.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO representing a temporary lock on a time slot.
 */
public record SlotLockDto(
        UUID lockId,
        OffsetDateTime start,
        OffsetDateTime end,
        OffsetDateTime expiresAt,
        String status
) {}
