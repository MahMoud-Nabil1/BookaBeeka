package com.system.booking.modules.availability.api.dto;

import java.time.LocalDate;

public record UpdateRoomBlockRequest(
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {}
