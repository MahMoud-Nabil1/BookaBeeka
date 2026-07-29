package com.system.booking.modules.availability.api;

import java.time.LocalTime;

/**
 * DTO representing a schedule rule.
 */
public record ScheduleRuleDto(
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {}
