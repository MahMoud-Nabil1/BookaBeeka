import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { mediaApi } from '../api/mediaApi';
import type { AttachPhotoRequest, ReorderPhotosRequest } from '../../../types/media';

// ── Query Hooks ───────────────────────────────────────────────────────────────

/**
 * Fetch all photos for a specific resource (room)
 */
export function useRoomPhotos(resourceId: string | undefined) {
  return useQuery({
    queryKey: ['photos', resourceId],
    queryFn: () => mediaApi.listPhotos(resourceId!),
    enabled: !!resourceId,
  });
}

// ── Mutation Hooks ────────────────────────────────────────────────────────────

/**
 * Upload a photo to Cloudinary and attach it to a resource
 * 
 * This handles the complete 3-step flow:
 * 1. Get signed upload parameters from backend
 * 2. Upload file directly to Cloudinary
 * 3. Attach the photo metadata to the resource
 */
export function useUploadPhoto(resourceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ file, isPrimary }: { file: File; isPrimary?: boolean }) => {
      // Step 1: Get upload signature from backend
      const signature = await mediaApi.getUploadSignature(resourceId);

      // Step 2: Upload to Cloudinary
      const formData = new FormData();
      formData.append('file', file);
      formData.append('signature', signature.signature);
      formData.append('timestamp', signature.timestamp.toString());
      formData.append('api_key', signature.apiKey);
      formData.append('folder', signature.folder);

      const cloudinaryUrl = `https://api.cloudinary.com/v1_1/${signature.cloudName}/image/upload`;
      
      const uploadResponse = await fetch(cloudinaryUrl, {
        method: 'POST',
        body: formData,
      });

      if (!uploadResponse.ok) {
        throw new Error('Failed to upload image to Cloudinary');
      }

      const cloudinaryData = await uploadResponse.json();

      // Step 3: Attach photo metadata to resource
      const attachRequest: AttachPhotoRequest = {
        url: cloudinaryData.secure_url,
        publicId: cloudinaryData.public_id,
        isPrimary,
      };

      return await mediaApi.attachPhoto(resourceId, attachRequest);
    },
    onSuccess: () => {
      toast.success('Photo uploaded successfully');
      queryClient.invalidateQueries({ queryKey: ['photos', resourceId] });
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to upload photo');
    },
  });
}

/**
 * Delete a photo from both backend and Cloudinary
 */
export function useDeletePhoto(resourceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (photoId: string) => mediaApi.deletePhoto(photoId),
    onSuccess: () => {
      toast.success('Photo deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['photos', resourceId] });
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to delete photo');
    },
  });
}

/**
 * Set a photo as the primary image for a resource
 */
export function useSetPrimaryPhoto(resourceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (photoId: string) => mediaApi.setPrimaryPhoto(photoId),
    onSuccess: () => {
      toast.success('Primary photo updated');
      queryClient.invalidateQueries({ queryKey: ['photos', resourceId] });
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to set primary photo');
    },
  });
}

/**
 * Reorder photos by providing an ordered array of photo IDs
 */
export function useReorderPhotos(resourceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ReorderPhotosRequest) =>
      mediaApi.reorderPhotos(resourceId, request),
    onSuccess: () => {
      toast.success('Photos reordered successfully');
      queryClient.invalidateQueries({ queryKey: ['photos', resourceId] });
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to reorder photos');
    },
  });
}
