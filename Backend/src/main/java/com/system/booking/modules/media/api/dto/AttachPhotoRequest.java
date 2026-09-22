package com.system.booking.modules.media.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to attach a Cloudinary-uploaded photo to a resource.
 *
 * <p>The {@code version} and {@code signature} fields are from Cloudinary's upload response
 * and are used for server-side verification via {@code verifyApiResponseSignature}.
 * Other metadata fields (secureUrl, width, height, bytes, format) are provided by the client
 * but will be overridden with authoritative values from the Cloudinary Admin API.</p>
 */
public record AttachPhotoRequest(
        @NotBlank String cloudinaryPublicId,
        @NotBlank String secureUrl,
        String originalFilename,
        String format,
        Integer width,
        Integer height,
        Long bytes,
        @NotBlank String version,
        @NotBlank String signature,
        String altText
) {}
