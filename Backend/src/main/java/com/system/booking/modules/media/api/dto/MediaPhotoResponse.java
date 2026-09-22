package com.system.booking.modules.media.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MediaPhotoResponse(
        UUID id,
        UUID resourceId,
        String secureUrl,
        String originalFilename,
        String format,
        Integer width,
        Integer height,
        Long bytes,
        Boolean isPrimary,
        Integer sortOrder,
        String altText,
        LocalDateTime createdAt
) {}
