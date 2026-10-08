package com.system.booking.modules.media.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Request to attach a Cloudinary-uploaded photo to a resource.
 *
 * <p>Supports both frontend naming conventions (url, publicId, isPrimary)
 * and backend naming conventions (cloudinaryPublicId, secureUrl, version, signature).
 * Metadata will be verified with authoritative values from the Cloudinary Admin API.</p>
 */
public record AttachPhotoRequest(
        @JsonAlias({"cloudinaryPublicId", "publicId"})
        String cloudinaryPublicId,

        @JsonAlias({"secureUrl", "url"})
        String secureUrl,

        String originalFilename,
        String format,
        Integer width,
        Integer height,
        Long bytes,
        String version,
        String signature,
        String altText,

        @JsonAlias("isPrimary")
        Boolean isPrimary
) {
    public AttachPhotoRequest(
            String cloudinaryPublicId,
            String secureUrl,
            String originalFilename,
            String format,
            Integer width,
            Integer height,
            Long bytes,
            String version,
            String signature,
            String altText
    ) {
        this(cloudinaryPublicId, secureUrl, originalFilename, format, width, height, bytes, version, signature, altText, null);
    }

    public String getEffectivePublicId() {
        return cloudinaryPublicId != null ? cloudinaryPublicId : "";
    }

    public String getEffectiveSecureUrl() {
        return secureUrl != null ? secureUrl : "";
    }
}
