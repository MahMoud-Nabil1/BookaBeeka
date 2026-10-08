package com.system.booking.modules.media.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MediaPhotoResponse(
        UUID id,
        UUID tenantId,
        UUID resourceId,
        String url,
        String publicId,
        String secureUrl,
        String cloudinaryPublicId,
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
