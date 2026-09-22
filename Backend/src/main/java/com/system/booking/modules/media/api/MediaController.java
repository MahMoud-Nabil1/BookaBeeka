package com.system.booking.modules.media.api;

import com.system.booking.modules.media.api.dto.AttachPhotoRequest;
import com.system.booking.modules.media.api.dto.MediaPhotoResponse;
import com.system.booking.modules.media.api.dto.ReorderPhotosRequest;
import com.system.booking.modules.media.api.dto.UploadSignatureResponse;
import com.system.booking.modules.media.internal.exception.CloudinaryException;
import com.system.booking.modules.media.internal.exception.PhotoLimitExceededException;
import com.system.booking.modules.media.internal.exception.PhotoNotFoundException;
import com.system.booking.modules.media.internal.service.MediaPhotoService;
import com.system.booking.modules.security.context.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for hotel room photo management.
 *
 * <p><b>Multi-Tenancy:</b> Admin endpoints extract tenantId from the JWT via
 * {@link TenantContextHolder} — never from the request. The public listing endpoint
 * validates resource visibility before returning photos.</p>
 *
 * <p><b>Access control</b> (enforced in SecurityConfig):<br>
 * - {@code GET /resources/{id}/photos} → public (permitAll)<br>
 * - All other endpoints → OWNER or ADMIN role required</p>
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaPhotoService mediaPhotoService;

    /**
     * Extracts the authenticated tenant's ID from the Security Context.
     */
    private UUID getTenantId() {
        return TenantContextHolder.getRequiredContext().tenantId();
    }

    // ── Upload Signature ──

    @PostMapping("/resources/{resourceId}/upload-signature")
    public ResponseEntity<UploadSignatureResponse> generateUploadSignature(
            @PathVariable UUID resourceId) {
        UploadSignatureResponse response = mediaPhotoService.generateUploadSignature(
                getTenantId(), resourceId);
        return ResponseEntity.ok(response);
    }

    // ── Attach Photo ──

    @PostMapping("/resources/{resourceId}/photos")
    public ResponseEntity<MediaPhotoResponse> attachPhoto(
            @PathVariable UUID resourceId,
            @Valid @RequestBody AttachPhotoRequest request) {
        MediaPhotoResponse response = mediaPhotoService.attachPhoto(
                getTenantId(), resourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── List Photos (public) ──

    @GetMapping("/resources/{resourceId}/photos")
    public ResponseEntity<List<MediaPhotoResponse>> listPhotos(
            @PathVariable UUID resourceId) {
        List<MediaPhotoResponse> photos = mediaPhotoService.listPhotosPublic(resourceId);
        return ResponseEntity.ok(photos);
    }

    // ── Delete Photo ──

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID photoId) {
        mediaPhotoService.deletePhoto(getTenantId(), photoId);
        return ResponseEntity.noContent().build();
    }

    // ── Set Primary ──

    @PatchMapping("/photos/{photoId}/primary")
    public ResponseEntity<MediaPhotoResponse> setPrimary(@PathVariable UUID photoId) {
        MediaPhotoResponse response = mediaPhotoService.setPrimary(getTenantId(), photoId);
        return ResponseEntity.ok(response);
    }

    // ── Reorder Photos ──

    @PatchMapping("/resources/{resourceId}/photos/reorder")
    public ResponseEntity<List<MediaPhotoResponse>> reorderPhotos(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ReorderPhotosRequest request) {
        List<MediaPhotoResponse> response = mediaPhotoService.reorderPhotos(
                getTenantId(), resourceId, request);
        return ResponseEntity.ok(response);
    }

    // ── Exception Handlers ──

    @ExceptionHandler(PhotoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PhotoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Not Found", "message", e.getMessage()));
    }

    @ExceptionHandler(CloudinaryException.class)
    public ResponseEntity<Map<String, String>> handleCloudinaryError(CloudinaryException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Cloudinary Error", "message", e.getMessage()));
    }

    @ExceptionHandler(PhotoLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handlePhotoLimit(PhotoLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Photo Limit Exceeded", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Bad Request", "message", e.getMessage()));
    }
}
