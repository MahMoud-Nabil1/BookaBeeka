package com.system.booking.modules.availability.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRoomBlockRequest(
        @NotNull UUID resourceId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
) {}
