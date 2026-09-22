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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaPhotoServiceTest {

    @Mock
    private MediaPhotoRepository mediaPhotoRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MediaPhotoService mediaPhotoService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();
    private Resource resource;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaPhotoService, "maxPhotosPerResource", 20);
        ReflectionTestUtils.setField(mediaPhotoService, "maxFileSizeBytes", 10485760L); // 10MB

        tenant = Tenant.builder()
                .id(tenantId)
                .name("Grand Hotel")
                .subdomain("grand-hotel")
                .status("ACTIVE")
                .build();

        resource = Resource.builder()
                .id(resourceId)
                .tenantId(tenantId)
                .name("Deluxe Room 101")
                .isActive(true)
                .isBookable(true)
                .build();
    }

    // ── 1. Upload Signature ──

    @Nested
    @DisplayName("generateUploadSignature")
    class GenerateUploadSignatureTests {

        @Test
        @DisplayName("should generate upload signature for own resource")
        void should_GenerateSignature_ForOwnResource() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(mediaPhotoRepository.countByResourceId(resourceId)).thenReturn(5L);

            UploadSignatureResponse expectedResponse = new UploadSignatureResponse(
                    "cloud", "key", 123456789L, "sig", "uuid", "folder"
            );
            when(cloudinaryService.generateUploadSignature(tenantId, resourceId))
                    .thenReturn(expectedResponse);

            UploadSignatureResponse response = mediaPhotoService.generateUploadSignature(tenantId, resourceId);

            assertThat(response).isEqualTo(expectedResponse);
            verify(resourceRepository).findByTenantIdAndId(tenantId, resourceId);
            verify(cloudinaryService).generateUploadSignature(tenantId, resourceId);
        }

        @Test
        @DisplayName("should reject signature generation when resource does not belong to tenant")
        void should_RejectSignature_WhenResourceNotOwnedByTenant() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> mediaPhotoService.generateUploadSignature(tenantId, resourceId))
                    .isInstanceOf(PhotoNotFoundException.class)
                    .hasMessageContaining("Resource not found for this tenant");

            verify(cloudinaryService, never()).generateUploadSignature(any(), any());
        }

        @Test
        @DisplayName("should reject signature generation when photo limit reached")
        void should_RejectSignature_WhenLimitReached() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(mediaPhotoRepository.countByResourceId(resourceId)).thenReturn(20L);

            assertThatThrownBy(() -> mediaPhotoService.generateUploadSignature(tenantId, resourceId))
                    .isInstanceOf(PhotoLimitExceededException.class)
                    .hasMessageContaining("Resource already has 20 photos (max: 20)");

            verify(cloudinaryService, never()).generateUploadSignature(any(), any());
        }
    }

    // ── 2. Attach Photo ──

    @Nested
    @DisplayName("attachPhoto")
    class AttachPhotoTests {

        private final String publicId = "bookabeeka/tenants/" + tenantId + "/resources/" + resourceId + "/img-1";
        private final AttachPhotoRequest request = new AttachPhotoRequest(
                publicId,
                "https://client-provided-url.com/img.jpg",
                "room.jpg",
                "jpg",
                100,
                100,
                5000L,
                "v1",
                "valid-sig",
                "Ocean view"
        );

        @Test
        @DisplayName("should attach photo with authoritative metadata from Cloudinary and auto-set first as primary")
        void should_AttachPhoto_WithAuthoritativeMetadata_AndFirstAsPrimary() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(true);
            when(cloudinaryService.isValidResourcePath(publicId, tenantId, resourceId))
                    .thenReturn(true);
            when(mediaPhotoRepository.existsByCloudinaryPublicId(publicId))
                    .thenReturn(false);

            // Authoritative metadata returned by Cloudinary Admin API
            Map<String, Object> authMeta = Map.of(
                    "format", "png",
                    "secure_url", "https://res.cloudinary.com/authoritative/img.png",
                    "width", 1920,
                    "height", 1080,
                    "bytes", 204800L
            );
            when(cloudinaryService.fetchAuthoritativeMetadata(publicId)).thenReturn(authMeta);
            when(mediaPhotoRepository.countByResourceId(resourceId)).thenReturn(0L);

            UUID photoId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            when(mediaPhotoRepository.save(any(MediaPhoto.class))).thenAnswer(invocation -> {
                MediaPhoto saved = invocation.getArgument(0);
                saved.setId(photoId);
                saved.setCreatedAt(now);
                return saved;
            });

            MediaPhotoResponse response = mediaPhotoService.attachPhoto(tenantId, resourceId, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(photoId);
            assertThat(response.resourceId()).isEqualTo(resourceId);
            // Authoritative values must override client-supplied values
            assertThat(response.secureUrl()).isEqualTo("https://res.cloudinary.com/authoritative/img.png");
            assertThat(response.format()).isEqualTo("png");
            assertThat(response.width()).isEqualTo(1920);
            assertThat(response.height()).isEqualTo(1080);
            assertThat(response.bytes()).isEqualTo(204800L);
            assertThat(response.isPrimary()).isTrue();
            assertThat(response.sortOrder()).isEqualTo(0);
            assertThat(response.altText()).isEqualTo("Ocean view");

            // Verify pessimistic lock was acquired on Resource
            verify(entityManager).find(Resource.class, resourceId, LockModeType.PESSIMISTIC_WRITE);
        }

        @Test
        @DisplayName("should not set subsequent photos as primary")
        void should_NotSetAsPrimary_WhenNotFirstPhoto() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(true);
            when(cloudinaryService.isValidResourcePath(publicId, tenantId, resourceId))
                    .thenReturn(true);
            when(mediaPhotoRepository.existsByCloudinaryPublicId(publicId))
                    .thenReturn(false);

            Map<String, Object> authMeta = Map.of(
                    "format", "jpg",
                    "secure_url", "https://res.cloudinary.com/authoritative/img.jpg",
                    "width", 1920,
                    "height", 1080,
                    "bytes", 204800L
            );
            when(cloudinaryService.fetchAuthoritativeMetadata(publicId)).thenReturn(authMeta);
            when(mediaPhotoRepository.countByResourceId(resourceId)).thenReturn(2L);

            when(mediaPhotoRepository.save(any(MediaPhoto.class))).thenAnswer(invocation -> {
                MediaPhoto saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            MediaPhotoResponse response = mediaPhotoService.attachPhoto(tenantId, resourceId, request);

            assertThat(response.isPrimary()).isFalse();
            assertThat(response.sortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("should reject attach when resource not owned by tenant")
        void should_RejectAttach_WhenResourceNotOwnedByTenant() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> mediaPhotoService.attachPhoto(tenantId, resourceId, request))
                    .isInstanceOf(PhotoNotFoundException.class);

            verify(cloudinaryService, never()).verifyUploadResponse(any(), any(), any());
        }

        @Test
        @DisplayName("should reject attach when Cloudinary response signature is invalid")
        void should_RejectAttach_WhenSignatureIsInvalid() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(false);

            assertThatThrownBy(() -> mediaPhotoService.attachPhoto(tenantId, resourceId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Cloudinary upload response signature");

            verify(mediaPhotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject attach when publicId does not match resource path")
        void should_RejectAttach_WhenPathMismatch() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(true);
            when(cloudinaryService.isValidResourcePath(publicId, tenantId, resourceId))
                    .thenReturn(false);

            assertThatThrownBy(() -> mediaPhotoService.attachPhoto(tenantId, resourceId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cloudinary public_id does not match the expected resource path");

            verify(mediaPhotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject attach and clean up Cloudinary asset when format is unsupported")
        void should_RejectAttach_AndCleanUpCloudinary_WhenFormatUnsupported() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(true);
            when(cloudinaryService.isValidResourcePath(publicId, tenantId, resourceId))
                    .thenReturn(true);
            when(mediaPhotoRepository.existsByCloudinaryPublicId(publicId)).thenReturn(false);

            Map<String, Object> authMeta = Map.of(
                    "format", "gif", // unsupported
                    "secure_url", "https://res.cloudinary.com/test.gif",
                    "width", 500,
                    "height", 500,
                    "bytes", 1000L
            );
            when(cloudinaryService.fetchAuthoritativeMetadata(publicId)).thenReturn(authMeta);

            assertThatThrownBy(() -> mediaPhotoService.attachPhoto(tenantId, resourceId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported image format: gif");

            verify(cloudinaryService).deleteAsset(publicId);
            verify(mediaPhotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject attach and clean up Cloudinary asset when file size exceeds max")
        void should_RejectAttach_AndCleanUpCloudinary_WhenFileSizeExceedsMax() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(true);
            when(cloudinaryService.isValidResourcePath(publicId, tenantId, resourceId))
                    .thenReturn(true);
            when(mediaPhotoRepository.existsByCloudinaryPublicId(publicId)).thenReturn(false);

            Map<String, Object> authMeta = Map.of(
                    "format", "jpg",
                    "secure_url", "https://res.cloudinary.com/test.jpg",
                    "width", 4000,
                    "height", 3000,
                    "bytes", 15000000L // 15MB > 10MB limit
            );
            when(cloudinaryService.fetchAuthoritativeMetadata(publicId)).thenReturn(authMeta);

            assertThatThrownBy(() -> mediaPhotoService.attachPhoto(tenantId, resourceId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds maximum allowed");

            verify(cloudinaryService).deleteAsset(publicId);
            verify(mediaPhotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject attach and clean up Cloudinary asset when photo limit reached under lock")
        void should_RejectAttach_AndCleanUpCloudinary_WhenLimitReachedUnderLock() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(cloudinaryService.verifyUploadResponse(publicId, "v1", "valid-sig"))
                    .thenReturn(true);
            when(cloudinaryService.isValidResourcePath(publicId, tenantId, resourceId))
                    .thenReturn(true);
            when(mediaPhotoRepository.existsByCloudinaryPublicId(publicId)).thenReturn(false);

            Map<String, Object> authMeta = Map.of(
                    "format", "jpg",
                    "secure_url", "https://res.cloudinary.com/test.jpg",
                    "width", 1920,
                    "height", 1080,
                    "bytes", 200000L
            );
            when(cloudinaryService.fetchAuthoritativeMetadata(publicId)).thenReturn(authMeta);
            when(mediaPhotoRepository.countByResourceId(resourceId)).thenReturn(20L); // at max

            assertThatThrownBy(() -> mediaPhotoService.attachPhoto(tenantId, resourceId, request))
                    .isInstanceOf(PhotoLimitExceededException.class)
                    .hasMessageContaining("already has 20 photos");

            verify(entityManager).find(Resource.class, resourceId, LockModeType.PESSIMISTIC_WRITE);
            verify(cloudinaryService).deleteAsset(publicId);
            verify(mediaPhotoRepository, never()).save(any());
        }
    }

    // ── 3. Public Listing ──

    @Nested
    @DisplayName("listPhotosPublic")
    class ListPhotosPublicTests {

        @Test
        @DisplayName("should return photos for active, bookable resource belonging to an active tenant")
        void should_ListPhotos_WhenResourceAndTenantActiveAndBookable() {
            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

            MediaPhoto photo1 = MediaPhoto.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .resource(resource)
                    .cloudinaryPublicId("p1")
                    .secureUrl("https://url1.jpg")
                    .isPrimary(true)
                    .sortOrder(0)
                    .build();
            photo1.setCreatedAt(LocalDateTime.now());

            when(mediaPhotoRepository.findByResourceIdOrderBySortOrderAsc(resourceId))
                    .thenReturn(List.of(photo1));

            List<MediaPhotoResponse> result = mediaPhotoService.listPhotosPublic(resourceId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).secureUrl()).isEqualTo("https://url1.jpg");
            assertThat(result.get(0).isPrimary()).isTrue();
        }

        @Test
        @DisplayName("should throw 404 when resource is not active")
        void should_Throw404_WhenResourceInactive() {
            resource.setIsActive(false);
            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

            assertThatThrownBy(() -> mediaPhotoService.listPhotosPublic(resourceId))
                    .isInstanceOf(PhotoNotFoundException.class)
                    .hasMessageContaining("Resource not found");
        }

        @Test
        @DisplayName("should throw 404 when resource is not bookable")
        void should_Throw404_WhenResourceNotBookable() {
            resource.setIsBookable(false);
            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

            assertThatThrownBy(() -> mediaPhotoService.listPhotosPublic(resourceId))
                    .isInstanceOf(PhotoNotFoundException.class)
                    .hasMessageContaining("Resource not found");
        }

        @Test
        @DisplayName("should throw 404 when tenant is not active")
        void should_Throw404_WhenTenantNotActive() {
            tenant.setStatus("SUSPENDED");
            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

            assertThatThrownBy(() -> mediaPhotoService.listPhotosPublic(resourceId))
                    .isInstanceOf(PhotoNotFoundException.class)
                    .hasMessageContaining("Resource not found");
        }

        @Test
        @DisplayName("should return empty list when resource has no photos")
        void should_ReturnEmptyList_WhenNoPhotos() {
            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(mediaPhotoRepository.findByResourceIdOrderBySortOrderAsc(resourceId))
                    .thenReturn(List.of());

            List<MediaPhotoResponse> result = mediaPhotoService.listPhotosPublic(resourceId);

            assertThat(result).isEmpty();
        }
    }

    // ── 4. Delete Photo ──

    @Nested
    @DisplayName("deletePhoto")
    class DeletePhotoTests {

        private final UUID photoId = UUID.randomUUID();
        private MediaPhoto photo;

        @BeforeEach
        void initPhoto() {
            photo = MediaPhoto.builder()
                    .id(photoId)
                    .tenantId(tenantId)
                    .resource(resource)
                    .cloudinaryPublicId("sample-cld-id")
                    .secureUrl("https://sample.jpg")
                    .isPrimary(true)
                    .sortOrder(0)
                    .build();
        }

        @Test
        @DisplayName("should delete Cloudinary asset first, then delete DB record, and promote next photo to primary")
        void should_DeleteCloudinaryThenDb_AndPromoteNextPrimary() {
            when(mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId))
                    .thenReturn(Optional.of(photo));

            MediaPhoto nextPhoto = MediaPhoto.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .resource(resource)
                    .cloudinaryPublicId("next-cld-id")
                    .secureUrl("https://next.jpg")
                    .isPrimary(false)
                    .sortOrder(1)
                    .build();

            when(mediaPhotoRepository.findByTenantIdAndResourceIdOrderBySortOrderAsc(tenantId, resourceId))
                    .thenReturn(List.of(nextPhoto));

            mediaPhotoService.deletePhoto(tenantId, photoId);

            // Step 1: Cloudinary delete
            verify(cloudinaryService).deleteAsset("sample-cld-id");
            // Step 2: DB delete
            verify(mediaPhotoRepository).delete(photo);
            // Step 3: Promote next photo
            assertThat(nextPhoto.getIsPrimary()).isTrue();
            verify(mediaPhotoRepository).save(nextPhoto);
        }

        @Test
        @DisplayName("should reject deletion when photo does not belong to tenant")
        void should_RejectDelete_WhenPhotoNotOwnedByTenant() {
            when(mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> mediaPhotoService.deletePhoto(tenantId, photoId))
                    .isInstanceOf(PhotoNotFoundException.class);

            verify(cloudinaryService, never()).deleteAsset(any());
            verify(mediaPhotoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should preserve DB row when Cloudinary deletion fails")
        void should_PreserveDbRow_WhenCloudinaryDeleteFails() {
            when(mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId))
                    .thenReturn(Optional.of(photo));
            org.mockito.Mockito.doThrow(new CloudinaryException("Network error"))
                    .when(cloudinaryService).deleteAsset("sample-cld-id");

            assertThatThrownBy(() -> mediaPhotoService.deletePhoto(tenantId, photoId))
                    .isInstanceOf(CloudinaryException.class);

            verify(mediaPhotoRepository, never()).delete(any());
        }
    }

    // ── 5. Set Primary ──

    @Nested
    @DisplayName("setPrimary")
    class SetPrimaryTests {

        private final UUID photoId = UUID.randomUUID();
        private final UUID oldPrimaryId = UUID.randomUUID();
        private MediaPhoto photo;
        private MediaPhoto oldPrimary;

        @BeforeEach
        void initPhotos() {
            oldPrimary = MediaPhoto.builder()
                    .id(oldPrimaryId)
                    .tenantId(tenantId)
                    .resource(resource)
                    .isPrimary(true)
                    .sortOrder(0)
                    .build();

            photo = MediaPhoto.builder()
                    .id(photoId)
                    .tenantId(tenantId)
                    .resource(resource)
                    .isPrimary(false)
                    .sortOrder(1)
                    .build();
            photo.setCreatedAt(LocalDateTime.now());
        }

        @Test
        @DisplayName("should unset old primary and set new primary")
        void should_UnsetOldPrimary_AndSetNew() {
            when(mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId))
                    .thenReturn(Optional.of(photo));
            when(mediaPhotoRepository.findByResourceIdAndIsPrimaryTrue(resourceId))
                    .thenReturn(Optional.of(oldPrimary));
            when(mediaPhotoRepository.save(any(MediaPhoto.class))).thenAnswer(inv -> inv.getArgument(0));

            MediaPhotoResponse response = mediaPhotoService.setPrimary(tenantId, photoId);

            assertThat(oldPrimary.getIsPrimary()).isFalse();
            verify(mediaPhotoRepository).save(oldPrimary);

            assertThat(photo.getIsPrimary()).isTrue();
            verify(mediaPhotoRepository).save(photo);
            assertThat(response.isPrimary()).isTrue();
        }

        @Test
        @DisplayName("should reject setPrimary when photo does not belong to tenant")
        void should_RejectSetPrimary_WhenNotOwnedByTenant() {
            when(mediaPhotoRepository.findByTenantIdAndId(tenantId, photoId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> mediaPhotoService.setPrimary(tenantId, photoId))
                    .isInstanceOf(PhotoNotFoundException.class);

            verify(mediaPhotoRepository, never()).save(any());
        }
    }

    // ── 6. Reorder Photos ──

    @Nested
    @DisplayName("reorderPhotos")
    class ReorderPhotosTests {

        private final UUID photo1Id = UUID.randomUUID();
        private final UUID photo2Id = UUID.randomUUID();
        private MediaPhoto photo1;
        private MediaPhoto photo2;

        @BeforeEach
        void initPhotos() {
            photo1 = MediaPhoto.builder()
                    .id(photo1Id)
                    .tenantId(tenantId)
                    .resource(resource)
                    .sortOrder(0)
                    .build();
            photo1.setCreatedAt(LocalDateTime.now());

            photo2 = MediaPhoto.builder()
                    .id(photo2Id)
                    .tenantId(tenantId)
                    .resource(resource)
                    .sortOrder(1)
                    .build();
            photo2.setCreatedAt(LocalDateTime.now());
        }

        @Test
        @DisplayName("should update sort orders when all photos belong to resource")
        void should_ReorderPhotos_Successfully() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(mediaPhotoRepository.findByTenantIdAndResourceIdOrderBySortOrderAsc(tenantId, resourceId))
                    .thenReturn(List.of(photo1, photo2));

            ReorderPhotosRequest request = new ReorderPhotosRequest(List.of(
                    new PhotoOrderEntry(photo1Id, 1),
                    new PhotoOrderEntry(photo2Id, 0)
            ));

            List<MediaPhotoResponse> result = mediaPhotoService.reorderPhotos(tenantId, resourceId, request);

            assertThat(photo1.getSortOrder()).isEqualTo(1);
            assertThat(photo2.getSortOrder()).isEqualTo(0);
            verify(mediaPhotoRepository).saveAll(List.of(photo1, photo2));
        }

        @Test
        @DisplayName("should reject reorder when a photo does not belong to resource")
        void should_RejectReorder_WhenPhotoNotBelongingToResource() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.of(resource));
            when(mediaPhotoRepository.findByTenantIdAndResourceIdOrderBySortOrderAsc(tenantId, resourceId))
                    .thenReturn(List.of(photo1)); // only photo1 belongs

            UUID foreignPhotoId = UUID.randomUUID();
            ReorderPhotosRequest request = new ReorderPhotosRequest(List.of(
                    new PhotoOrderEntry(foreignPhotoId, 0)
            ));

            assertThatThrownBy(() -> mediaPhotoService.reorderPhotos(tenantId, resourceId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to resource");

            verify(mediaPhotoRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("should reject reorder when resource does not belong to tenant")
        void should_RejectReorder_WhenResourceNotOwnedByTenant() {
            when(resourceRepository.findByTenantIdAndId(tenantId, resourceId))
                    .thenReturn(Optional.empty());

            ReorderPhotosRequest request = new ReorderPhotosRequest(List.of(
                    new PhotoOrderEntry(photo1Id, 0)
            ));

            assertThatThrownBy(() -> mediaPhotoService.reorderPhotos(tenantId, resourceId, request))
                    .isInstanceOf(PhotoNotFoundException.class);
        }
    }
}
