import api from '../../../config/api';
import type {
  MediaPhotoResponse,
  AttachPhotoRequest,
  UploadSignatureResponse,
  ReorderPhotosRequest,
} from '../../../types/media';

// ── Media API ─────────────────────────────────────────────────────────────────
// Handles photo management for resources (rooms) with Cloudinary integration

export const mediaApi = {
  // POST /api/media/resources/{resourceId}/upload-signature
  // Get signed Cloudinary upload parameters
  getUploadSignature: async (resourceId: string): Promise<UploadSignatureResponse> => {
    const response = await api.post<UploadSignatureResponse>(
      `/api/media/resources/${resourceId}/upload-signature`
    );
    return response.data;
  },

  // POST /api/media/resources/{resourceId}/photos
  // Attach uploaded photo metadata after Cloudinary upload completes
  attachPhoto: async (
    resourceId: string,
    req: AttachPhotoRequest
  ): Promise<MediaPhotoResponse> => {
    const response = await api.post<MediaPhotoResponse>(
      `/api/media/resources/${resourceId}/photos`,
      req
    );
    return response.data;
  },

  // GET /api/media/resources/{resourceId}/photos
  // List all photos for a resource
  listPhotos: async (resourceId: string): Promise<MediaPhotoResponse[]> => {
    const response = await api.get<MediaPhotoResponse[]>(
      `/api/media/resources/${resourceId}/photos`
    );
    return response.data;
  },

  // DELETE /api/media/photos/{photoId}
  // Delete a photo (also removes from Cloudinary)
  deletePhoto: async (photoId: string): Promise<void> => {
    await api.delete(`/api/media/photos/${photoId}`);
  },

  // PATCH /api/media/photos/{photoId}/primary
  // Set a photo as the primary image for its resource
  setPrimaryPhoto: async (photoId: string): Promise<MediaPhotoResponse> => {
    const response = await api.patch<MediaPhotoResponse>(
      `/api/media/photos/${photoId}/primary`
    );
    return response.data;
  },

  // PATCH /api/media/resources/{resourceId}/photos/reorder
  // Reorder photos by providing ordered array of photo IDs
  reorderPhotos: async (
    resourceId: string,
    req: ReorderPhotosRequest
  ): Promise<MediaPhotoResponse[]> => {
    const response = await api.patch<MediaPhotoResponse[]>(
      `/api/media/resources/${resourceId}/photos/reorder`,
      req
    );
    return response.data;
  },
};
