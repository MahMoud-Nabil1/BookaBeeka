package com.system.booking.modules.availability.api;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing an availability exception.
 */
public record ExceptionDto(
        LocalDate exceptionDate,
        Boolean isAvailable,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {}
