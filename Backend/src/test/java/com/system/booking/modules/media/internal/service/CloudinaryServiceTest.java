package com.system.booking.modules.media.internal.service;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Configuration;
import com.cloudinary.Uploader;
import com.system.booking.modules.media.api.dto.UploadSignatureResponse;
import com.system.booking.modules.media.internal.exception.CloudinaryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    private CloudinaryService cloudinaryService;

    private final Configuration config = new Configuration();

    @BeforeEach
    void setUp() throws Exception {
        config.cloudName = "test-cloud";
        config.apiKey = "test-api-key";
        config.apiSecret = "test-api-secret";
        config.signatureVersion = 1;

        // Cloudinary.config is a public final field; set it on the mock instance via reflection
        Field configField = Cloudinary.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(cloudinary, config);

        cloudinaryService = new CloudinaryService(cloudinary);
    }

    @Nested
    @DisplayName("generateUploadSignature")
    class GenerateUploadSignatureTests {

        @Test
        @DisplayName("should return signed parameters with expected folder and without secret")
        void should_GenerateUploadSignature_WithCorrectParams() {
            UUID tenantId = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();

            when(cloudinary.apiSignRequest(anyMap(), eq("test-api-secret"), eq(1)))
                    .thenReturn("mock-signature-hash");

            UploadSignatureResponse response = cloudinaryService.generateUploadSignature(tenantId, resourceId);

            assertThat(response).isNotNull();
            assertThat(response.cloudName()).isEqualTo("test-cloud");
            assertThat(response.apiKey()).isEqualTo("test-api-key");
            assertThat(response.signature()).isEqualTo("mock-signature-hash");
            assertThat(response.folder()).isEqualTo("bookabeeka/tenants/" + tenantId + "/resources/" + resourceId);
            assertThat(response.publicId()).isNotBlank();
            assertThat(response.timestamp()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("verifyUploadResponse")
    class VerifyUploadResponseTests {

        @Test
        @DisplayName("should return true when signature is valid")
        void should_ReturnTrue_WhenSignatureIsValid() {
            when(cloudinary.verifyApiResponseSignature("sample_id", "12345", "valid_sig"))
                    .thenReturn(true);

            boolean result = cloudinaryService.verifyUploadResponse("sample_id", "12345", "valid_sig");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when signature is invalid")
        void should_ReturnFalse_WhenSignatureIsInvalid() {
            when(cloudinary.verifyApiResponseSignature("sample_id", "12345", "invalid_sig"))
                    .thenReturn(false);

            boolean result = cloudinaryService.verifyUploadResponse("sample_id", "12345", "invalid_sig");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when verification throws an exception")
        void should_ReturnFalse_WhenVerificationThrowsException() {
            when(cloudinary.verifyApiResponseSignature(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Verification service unavailable"));

            boolean result = cloudinaryService.verifyUploadResponse("sample_id", "12345", "sig");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("fetchAuthoritativeMetadata")
    class FetchAuthoritativeMetadataTests {

        @Test
        @DisplayName("should return metadata map from Cloudinary Admin API")
        void should_ReturnMetadata_WhenApiCallSucceeds() throws Exception {
            Api apiMock = mock(Api.class);
            when(cloudinary.api()).thenReturn(apiMock);

            com.cloudinary.api.ApiResponse apiResponse = mock(com.cloudinary.api.ApiResponse.class);
            when(apiResponse.get("format")).thenReturn("jpg");
            when(apiResponse.get("secure_url")).thenReturn("https://res.cloudinary.com/test/image.jpg");
            when(apiResponse.get("width")).thenReturn(1920);
            when(apiResponse.get("height")).thenReturn(1080);
            when(apiResponse.get("bytes")).thenReturn(204800L);

            when(apiMock.resource(eq("sample_public_id"), anyMap())).thenReturn(apiResponse);

            Map<String, Object> result = cloudinaryService.fetchAuthoritativeMetadata("sample_public_id");

            assertThat(result).isNotNull();
            assertThat(result.get("format")).isEqualTo("jpg");
            assertThat(result.get("secure_url")).isEqualTo("https://res.cloudinary.com/test/image.jpg");
            assertThat(result.get("width")).isEqualTo(1920);
            assertThat(result.get("height")).isEqualTo(1080);
            assertThat(result.get("bytes")).isEqualTo(204800L);
            verify(apiMock).resource(eq("sample_public_id"), anyMap());
        }

        @Test
        @DisplayName("should throw CloudinaryException when Admin API fails")
        void should_ThrowCloudinaryException_WhenApiCallFails() throws Exception {
            Api apiMock = mock(Api.class);
            when(cloudinary.api()).thenReturn(apiMock);
            when(apiMock.resource(eq("non_existent_id"), anyMap()))
                    .thenThrow(new RuntimeException("Resource not found"));

            assertThatThrownBy(() -> cloudinaryService.fetchAuthoritativeMetadata("non_existent_id"))
                    .isInstanceOf(CloudinaryException.class)
                    .hasMessageContaining("Failed to verify asset in Cloudinary");
        }
    }

    @Nested
    @DisplayName("deleteAsset")
    class DeleteAssetTests {

        @Test
        @DisplayName("should succeed when deletion result is 'ok'")
        void should_Succeed_WhenResultIsOk() throws Exception {
            Uploader uploaderMock = mock(Uploader.class);
            when(cloudinary.uploader()).thenReturn(uploaderMock);
            when(uploaderMock.destroy(eq("sample_id"), anyMap())).thenReturn(Map.of("result", "ok"));

            cloudinaryService.deleteAsset("sample_id");

            verify(uploaderMock).destroy(eq("sample_id"), anyMap());
        }

        @Test
        @DisplayName("should succeed when deletion result is 'not found' (idempotent)")
        void should_Succeed_WhenResultIsNotFound() throws Exception {
            Uploader uploaderMock = mock(Uploader.class);
            when(cloudinary.uploader()).thenReturn(uploaderMock);
            when(uploaderMock.destroy(eq("sample_id"), anyMap())).thenReturn(Map.of("result", "not found"));

            cloudinaryService.deleteAsset("sample_id");

            verify(uploaderMock).destroy(eq("sample_id"), anyMap());
        }

        @Test
        @DisplayName("should throw CloudinaryException when result is unexpected")
        void should_ThrowException_WhenResultIsUnexpected() throws Exception {
            Uploader uploaderMock = mock(Uploader.class);
            when(cloudinary.uploader()).thenReturn(uploaderMock);
            when(uploaderMock.destroy(eq("sample_id"), anyMap())).thenReturn(Map.of("result", "error"));

            assertThatThrownBy(() -> cloudinaryService.deleteAsset("sample_id"))
                    .isInstanceOf(CloudinaryException.class)
                    .hasMessageContaining("Unexpected Cloudinary delete result");
        }

        @Test
        @DisplayName("should throw CloudinaryException when uploader throws exception")
        void should_ThrowException_WhenUploaderFails() throws Exception {
            Uploader uploaderMock = mock(Uploader.class);
            when(cloudinary.uploader()).thenReturn(uploaderMock);
            when(uploaderMock.destroy(eq("sample_id"), anyMap()))
                    .thenThrow(new RuntimeException("Network timeout"));

            assertThatThrownBy(() -> cloudinaryService.deleteAsset("sample_id"))
                    .isInstanceOf(CloudinaryException.class)
                    .hasMessageContaining("Failed to delete Cloudinary asset");
        }
    }

    @Nested
    @DisplayName("isValidResourcePath")
    class IsValidResourcePathTests {

        private final UUID tenantId = UUID.randomUUID();
        private final UUID resourceId = UUID.randomUUID();

        @Test
        @DisplayName("should return true for matching tenant and resource path")
        void should_ReturnTrue_ForMatchingPath() {
            String validPath = "bookabeeka/tenants/" + tenantId + "/resources/" + resourceId + "/asset-123";

            boolean result = cloudinaryService.isValidResourcePath(validPath, tenantId, resourceId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for different tenant id")
        void should_ReturnFalse_ForDifferentTenant() {
            UUID otherTenant = UUID.randomUUID();
            String path = "bookabeeka/tenants/" + otherTenant + "/resources/" + resourceId + "/asset-123";

            boolean result = cloudinaryService.isValidResourcePath(path, tenantId, resourceId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for different resource id under same tenant")
        void should_ReturnFalse_ForDifferentResource() {
            UUID otherResource = UUID.randomUUID();
            String path = "bookabeeka/tenants/" + tenantId + "/resources/" + otherResource + "/asset-123";

            boolean result = cloudinaryService.isValidResourcePath(path, tenantId, resourceId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for invalid root path")
        void should_ReturnFalse_ForInvalidRoot() {
            String path = "otherprefix/tenants/" + tenantId + "/resources/" + resourceId + "/asset-123";

            boolean result = cloudinaryService.isValidResourcePath(path, tenantId, resourceId);

            assertThat(result).isFalse();
        }
    }
}
