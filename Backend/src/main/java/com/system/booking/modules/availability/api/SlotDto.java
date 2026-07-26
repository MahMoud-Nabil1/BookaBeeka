package com.system.booking.modules.availability.api;

import java.time.OffsetDateTime;

/**
 * DTO representing an available time slot.
 */
public record SlotDto(
        OffsetDateTime start,
        OffsetDateTime end
) {}
