package com.system.booking.modules.media.internal.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.system.booking.modules.media.api.dto.UploadSignatureResponse;
import com.system.booking.modules.media.internal.exception.CloudinaryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Handles all Cloudinary operations: signature generation, response verification,
 * authoritative metadata retrieval, and asset deletion.
 *
 * <p>The Cloudinary API secret is used server-side only — never exposed to clients.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final String FOLDER_PREFIX = "bookabeeka/tenants";
    private static final String ALLOWED_FORMATS = "jpg,jpeg,png,webp,avif";

    /**
     * Generates signed upload parameters for direct frontend → Cloudinary upload.
     *
     * @param tenantId   the authenticated tenant
     * @param resourceId the resource (room) the photo will belong to
     * @return signed parameters (never includes apiSecret)
     */
    public UploadSignatureResponse generateUploadSignature(UUID tenantId, UUID resourceId) {
        String folder = FOLDER_PREFIX + "/" + tenantId + "/resources/" + resourceId;
        String publicId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis() / 1000L;

        // Parameters that will be signed — must match what the frontend sends to Cloudinary
        Map<String, Object> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", timestamp);

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret,
                cloudinary.config.signatureVersion);

        return new UploadSignatureResponse(
                cloudinary.config.cloudName,
                cloudinary.config.apiKey,
                timestamp,
                signature,
                publicId,
                folder
        );
    }

    /**
     * Verifies the Cloudinary upload response signature to confirm the upload is genuine.
     *
     * <p>Uses {@code verifyApiResponseSignature(publicId, version, signature)} which
     * hashes {@code publicId + version} with our {@code apiSecret} and compares.</p>
     *
     * @param publicId  the public_id from Cloudinary's upload response
     * @param version   the version from Cloudinary's upload response
     * @param signature the signature from Cloudinary's upload response
     * @return true if the signature is valid
     */
    public boolean verifyUploadResponse(String publicId, String version, String signature) {
        try {
            return cloudinary.verifyApiResponseSignature(publicId, version, signature);
        } catch (Exception e) {
            log.warn("Cloudinary response signature verification failed for publicId={}: {}",
                    publicId, e.getMessage());
            return false;
        }
    }

    /**
     * Fetches authoritative asset metadata from the Cloudinary Admin API.
     *
     * <p>This is the source of truth for width, height, bytes, format, and secure_url.
     * Client-provided values are NOT trusted — they are replaced with these.</p>
     *
     * @param cloudinaryPublicId the full public_id (including folder path)
     * @return map of authoritative metadata from Cloudinary
     * @throws CloudinaryException if the API call fails or the asset does not exist
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchAuthoritativeMetadata(String cloudinaryPublicId) {
        try {
            Map<String, Object> result = cloudinary.api().resource(
                    cloudinaryPublicId,
                    ObjectUtils.asMap("resource_type", "image")
            );
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch Cloudinary metadata for publicId={}: {}",
                    cloudinaryPublicId, e.getMessage());
            throw new CloudinaryException(
                    "Failed to verify asset in Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a Cloudinary asset by its public_id.
     *
     * @param cloudinaryPublicId the full public_id to delete
     * @throws CloudinaryException if the deletion fails
     */
    @SuppressWarnings("unchecked")
    public void deleteAsset(String cloudinaryPublicId) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(
                    cloudinaryPublicId,
                    ObjectUtils.asMap("resource_type", "image")
            );
            String deleteResult = (String) result.get("result");
            if (!"ok".equals(deleteResult) && !"not found".equals(deleteResult)) {
                throw new CloudinaryException(
                        "Unexpected Cloudinary delete result: " + deleteResult);
            }
            log.info("Cloudinary asset deleted: publicId={}, result={}", cloudinaryPublicId, deleteResult);
        } catch (CloudinaryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete Cloudinary asset publicId={}: {}",
                    cloudinaryPublicId, e.getMessage());
            throw new CloudinaryException(
                    "Failed to delete Cloudinary asset: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the cloudinaryPublicId belongs to the expected tenant and resource path.
     *
     * <p>Expected format: {@code bookabeeka/tenants/{tenantId}/resources/{resourceId}/{uuid}}</p>
     */
    public boolean isValidResourcePath(String cloudinaryPublicId, UUID tenantId, UUID resourceId) {
        String expectedPrefix = FOLDER_PREFIX + "/" + tenantId + "/resources/" + resourceId + "/";
        return cloudinaryPublicId.startsWith(expectedPrefix);
    }
}
