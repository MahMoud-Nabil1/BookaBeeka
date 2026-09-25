package com.system.booking.modules.media.internal.service;

import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.media.api.dto.AttachPhotoRequest;
import com.system.booking.modules.media.api.dto.MediaPhotoResponse;
import com.system.booking.modules.media.api.dto.PhotoOrderEntry;
import com.system.booking.modules.media.api.dto.ReorderPhotosRequest;
import com.system.booking.modules.media.api.dto.UploadSignatureResponse;
import com.system.booking.modules.media.internal.entity.MediaPhoto;
import com.system.booking.modules.media.internal.exception.CloudinaryException;
import com.system.booking.modules.media.internal.exception.PhotoLimitExceededException;
import com.system.booking.modules.media.internal.exception.PhotoNotFoundException;
import com.system.booking.modules.media.internal.repository.MediaPhotoRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core service for hotel room photo management.
 *
 * <p><b>Tenant isolation:</b> Every mutating operation validates
 * {@code currentTenant == resource.tenantId}. Tenant is always from JWT, never from client.</p>
 *
 * <p><b>Metadata trust:</b> Client-provided metadata (width, height, bytes, format, secureUrl)
 * is NOT trusted. Authoritative values are fetched from the Cloudinary Admin API after
 * verifying the upload response signature.</p>
 *
 * <p><b>Concurrency:</b> Photo count limit is enforced under a pessimistic lock on the
 * Resource row to prevent concurrent uploads from exceeding the max.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaPhotoService {

    private final MediaPhotoRepository mediaPhotoRepository;
    private final CloudinaryService cloudinaryService;
    private final ResourceRepository resourceRepository;
    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;

    @Value("${media.max-photos-per-resource:20}")
    private int maxPhotosPerResource;

    @Value("${media.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    private static final Set<String> ALLOWED_FORMATS = Set.of("jpg", "jpeg", "png", "webp", "avif");

    // ── Upload Signature ──

    /**
     * Generates Cloudinary signed upload parameters for an admin uploading a room photo.
     */
    @Transactional(readOnly = true)
    public UploadSignatureResponse generateUploadSignature(UUID tenantId, UUID resourceId) {
        Resource resource = findResourceForTenant(tenantId, resourceId);

        // Check photo count before generating signature (lightweight pre-check)
        long currentCount = mediaPhotoRepository.countByResourceId(resourceId);
        if (currentCount >= maxPhotosPerResource) {
            throw new PhotoLimitExceededException(
                    "Resource already has " + currentCount + " photos (max: " + maxPhotosPerResource + ")");
        }

        return cloudinaryService.generateUploadSignature(tenantId, resourceId);
    }

    // ── Attach Photo ──

    /**
     * Attaches a Cloudinary-uploaded photo to a resource.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate tenant owns the resource</li>
     *   <li>Verify Cloudinary upload response signature</li>
     *   <li>Validate cloudinaryPublicId matches exact resource path</li>
     *   <li>Fetch authoritative metadata from Cloudinary Admin API</li>
     *   <li>Validate format and file size from authoritative data</li>
     *   <li>Acquire pessimistic lock on Resource and check photo count</li>
     *   <li>Persist MediaPhoto with authoritative metadata</li>
     * </ol></p>
     */
    @Transactional
    public MediaPhotoResponse attachPhoto(UUID tenantId, UUID resourceId, AttachPhotoRequest request) {
        // 1. Validate tenant owns the resource
        Resource resource = findResourceForTenant(tenantId, resourceId);

        // 2. Verify Cloudinary upload response signature
        boolean signatureValid = cloudinaryService.verifyUploadResponse(
                request.cloudinaryPublicId(), request.version(), request.signature());
        if (!signatureValid) {
            throw new IllegalArgumentException("Invalid Cloudinary upload response signature");
        }

        // 3. Validate cloudinaryPublicId matches exact tenant + resource path
        if (!cloudinaryService.isValidResourcePath(request.cloudinaryPublicId(), tenantId, resourceId)) {
            throw new IllegalArgumentException(
                    "Cloudinary public_id does not match the expected resource path");
        }

        // 4. Check for duplicate public_id
        if (mediaPhotoRepository.existsByCloudinaryPublicId(request.cloudinaryPublicId())) {
            throw new IllegalArgumentException("Photo with this Cloudinary public_id already exists");
        }

        // 5. Fetch authoritative metadata from Cloudinary Admin API
        Map<String, Object> authMetadata = cloudinaryService.fetchAuthoritativeMetadata(
                request.cloudinaryPublicId());

        String authFormat = (String) authMetadata.get("format");
        String authSecureUrl = (String) authMetadata.get("secure_url");
        Integer authWidth = authMetadata.get("width") != null
                ? ((Number) authMetadata.get("width")).intValue() : null;
        Integer authHeight = authMetadata.get("height") != null
                ? ((Number) authMetadata.get("height")).intValue() : null;
        Long authBytes = authMetadata.get("bytes") != null
                ? ((Number) authMetadata.get("bytes")).longValue() : null;

        // 6. Validate format from authoritative data
        if (authFormat == null || !ALLOWED_FORMATS.contains(authFormat.toLowerCase())) {
            // Delete the unsupported asset from Cloudinary
            try { cloudinaryService.deleteAsset(request.cloudinaryPublicId()); } catch (Exception ignored) {}
            throw new IllegalArgumentException(
                    "Unsupported image format: " + authFormat + ". Allowed: " + ALLOWED_FORMATS);
        }

        // 7. Validate file size from authoritative data
        if (authBytes != null && authBytes > maxFileSizeBytes) {
            // Delete the oversized asset from Cloudinary
            try { cloudinaryService.deleteAsset(request.cloudinaryPublicId()); } catch (Exception ignored) {}
            throw new IllegalArgumentException(
                    "File size " + authBytes + " bytes exceeds maximum allowed " + maxFileSizeBytes + " bytes");
        }

        // 8. Pessimistic lock on Resource row to serialize concurrent uploads
        entityManager.find(Resource.class, resourceId, LockModeType.PESSIMISTIC_WRITE);

        long currentCount = mediaPhotoRepository.countByResourceId(resourceId);
        if (currentCount >= maxPhotosPerResource) {
            // Delete the asset from Cloudinary since we can't store it
            try { cloudinaryService.deleteAsset(request.cloudinaryPublicId()); } catch (Exception ignored) {}
            throw new PhotoLimitExceededException(
                    "Resource already has " + currentCount + " photos (max: " + maxPhotosPerResource + ")");
        }

        // 9. First photo for this resource is automatically primary
        boolean isFirstPhoto = (currentCount == 0);

        // 10. Persist with authoritative metadata
        MediaPhoto photo = MediaPhoto.builder()
                .tenantId(tenantId)
                .resource(resource)
                .cloudinaryPublicId(request.cloudinaryPublicId())
                .secureUrl(authSecureUrl)
                .originalFilename(request.originalFilename())
                .format(authFormat)
                .width(authWidth)
                .height(authHeight)
                .bytes(authBytes)
                .isPrimary(isFirstPhoto)
                .sortOrder((int) currentCount)
                .altText(request.altText())
                .build();

        photo = mediaPhotoRepository.save(photo);
        log.info("Photo attached: id={}, resourceId={}, tenantId={}", photo.getId(), resourceId, tenantId);

        return toResponse(photo);
    }

    // ── List Photos (public) ──

    /**
     * Lists photos for a publicly visible resource.
     *
     * <p>Validates that the resource exists, is active, is bookable,
     * and belongs to an active tenant before returning photos.</p>
     */
    @Transactional(readOnly = true)
    public List<MediaPhotoResponse> listPhotosPublic(UUID resourceId) {
        // Validate resource exists and is publicly visible
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new PhotoNotFoundException("Resource not found"));

        if (!Boolean.TRUE.equals(resource.getIsActive())
                || !Boolean.TRUE.equals(resource.getIsBookable())) {
            throw new PhotoNotFoundException("Resource not found");
        }

        // Validate tenant is active
        Tenant tenant = tenantRepository.findById(resource.getTenantId())
                .orElseThrow(() -> new PhotoNotFoundException("Resource not found"));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new PhotoNotFoundException("Resource not found");
        }

        return mediaPhotoRepository.findByResourceIdOrderBySortOrderAsc(resourceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Delete Photo ──

    /**
     * Deletes a photo: Cloudinary asset first, then DB row.
     * If Cloudinary deletion fails, DB row is preserved for retry.
     */
    @Transactional
    public void deletePhoto(UUID tenantId, UUID photoId) {
        MediaPhoto photo = mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found"));

        // Step 1: Delete from Cloudinary first
        cloudinaryService.deleteAsset(photo.getCloudinaryPublicId());

        // Step 2: Delete from DB (only if Cloudinary succeeded)
        boolean wasPrimary = Boolean.TRUE.equals(photo.getIsPrimary());
        UUID resourceId = photo.getResource().getId();
        mediaPhotoRepository.delete(photo);

        // If the deleted photo was primary, promote the next one
        if (wasPrimary) {
            List<MediaPhoto> remaining = mediaPhotoRepository
                    .findByTenantIdAndResourceIdOrderBySortOrderAsc(tenantId, resourceId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsPrimary(true);
                mediaPhotoRepository.save(remaining.get(0));
            }
        }

        log.info("Photo deleted: id={}, tenantId={}", photoId, tenantId);
    }

    // ── Set Primary ──

    /**
     * Sets a photo as the primary photo for its resource.
     * The previous primary (if any) is automatically unset.
     */
    @Transactional
    public MediaPhotoResponse setPrimary(UUID tenantId, UUID photoId) {
        MediaPhoto photo = mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found"));

        UUID resourceId = photo.getResource().getId();

        // Unset the current primary
        Optional<MediaPhoto> currentPrimary = mediaPhotoRepository
                .findByResourceIdAndIsPrimaryTrue(resourceId);
        currentPrimary.ifPresent(p -> {
            p.setIsPrimary(false);
            mediaPhotoRepository.save(p);
        });

        // Set new primary
        photo.setIsPrimary(true);
        photo = mediaPhotoRepository.save(photo);

        log.info("Primary photo set: photoId={}, resourceId={}", photoId, resourceId);
        return toResponse(photo);
    }

    // ── Reorder Photos ──

    /**
     * Reorders photos for a resource. All photos in the request must belong to the
     * same resource and current tenant.
     */
    @Transactional
    public List<MediaPhotoResponse> reorderPhotos(UUID tenantId, UUID resourceId, ReorderPhotosRequest request) {
        // Validate resource belongs to tenant
        findResourceForTenant(tenantId, resourceId);

        // Fetch all photos for this resource
        List<MediaPhoto> existingPhotos = mediaPhotoRepository
                .findByTenantIdAndResourceIdOrderBySortOrderAsc(tenantId, resourceId);

        Set<UUID> existingIds = existingPhotos.stream()
                .map(MediaPhoto::getId)
                .collect(Collectors.toSet());

        // Validate all requested photo IDs belong to this resource
        for (PhotoOrderEntry entry : request.photos()) {
            if (!existingIds.contains(entry.photoId())) {
                throw new IllegalArgumentException(
                        "Photo " + entry.photoId() + " does not belong to resource " + resourceId);
            }
        }

        // Apply new sort orders
        Map<UUID, Integer> newOrders = request.photos().stream()
                .collect(Collectors.toMap(PhotoOrderEntry::photoId, PhotoOrderEntry::sortOrder));

        for (MediaPhoto photo : existingPhotos) {
            Integer newOrder = newOrders.get(photo.getId());
            if (newOrder != null) {
                photo.setSortOrder(newOrder);
            }
        }
        mediaPhotoRepository.saveAll(existingPhotos);

        log.info("Photos reordered: resourceId={}, count={}", resourceId, request.photos().size());

        return mediaPhotoRepository
                .findByTenantIdAndResourceIdOrderBySortOrderAsc(tenantId, resourceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Private Helpers ──

    private Resource findResourceForTenant(UUID tenantId, UUID resourceId) {
        return resourceRepository.findByTenantIdAndId(tenantId, resourceId)
                .orElseThrow(() -> new PhotoNotFoundException(
                        "Resource not found for this tenant"));
    }

    private MediaPhotoResponse toResponse(MediaPhoto photo) {
        return new MediaPhotoResponse(
                photo.getId(),
                photo.getResource().getId(),
                photo.getSecureUrl(),
                photo.getOriginalFilename(),
                photo.getFormat(),
                photo.getWidth(),
                photo.getHeight(),
                photo.getBytes(),
                photo.getIsPrimary(),
                photo.getSortOrder(),
                photo.getAltText(),
                photo.getCreatedAt()
        );
    }
}
