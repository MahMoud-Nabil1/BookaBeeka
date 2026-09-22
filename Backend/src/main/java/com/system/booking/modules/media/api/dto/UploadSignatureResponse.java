package com.system.booking.modules.media.api.dto;

public record UploadSignatureResponse(
        String cloudName,
        String apiKey,
        long timestamp,
        String signature,
        String publicId,
        String folder
) {}
