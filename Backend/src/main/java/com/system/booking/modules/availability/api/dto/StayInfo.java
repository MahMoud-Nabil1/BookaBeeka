package com.system.booking.modules.availability.api.dto;

import java.time.LocalDate;

public record StayInfo(
        LocalDate checkIn,
        LocalDate checkOut,
        int nights
) {}
