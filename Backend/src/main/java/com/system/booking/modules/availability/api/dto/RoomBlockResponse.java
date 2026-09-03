package com.system.booking.modules.availability.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RoomBlockResponse(
        UUID id,
        UUID resourceId,
        String roomName,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LocalDateTime createdAt
) {}
