import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { amenityApi } from '../api/amenityApi';
import type {
  CreateAmenityRequest,
  UpdateAmenityRequest,
} from '../../../types/inventory';
import { useSelector } from 'react-redux';
import type { RootState } from '@/redux';

export function useAmenities() {
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useQuery({
    queryKey: ['amenities', tenantId],
    queryFn: () => amenityApi.listAmenities(tenantId!),
    enabled: !!tenantId,
  });
}

export function useRoomAmenities(roomId: string) {
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useQuery({
    queryKey: ['amenities', 'room', roomId, tenantId],
    queryFn: () => amenityApi.listRoomAmenities(roomId, tenantId!),
    enabled: !!tenantId && !!roomId,
  });
}

export function useCreateAmenity() {
  const queryClient = useQueryClient();
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useMutation({
    mutationFn: (req: CreateAmenityRequest) =>
      amenityApi.createAmenity(req, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['amenities'] });
      toast.success('Amenity created successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create amenity');
    },
  });
}

export function useUpdateAmenity() {
  const queryClient = useQueryClient();
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateAmenityRequest }) =>
      amenityApi.updateAmenity(id, req, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['amenities'] });
      toast.success('Amenity updated successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to update amenity');
    },
  });
}

export function useDeleteAmenity() {
  const queryClient = useQueryClient();
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useMutation({
    mutationFn: (id: string) => amenityApi.deleteAmenity(id, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['amenities'] });
      toast.success('Amenity deleted successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to delete amenity');
    },
  });
}

export function useLinkAmenity(roomId: string) {
  const queryClient = useQueryClient();
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useMutation({
    mutationFn: (amenityId: string) =>
      amenityApi.linkAmenityToRoom(roomId, amenityId, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['amenities', 'room', roomId] });
      toast.success('Amenity linked to room');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Failed to link amenity to room'
      );
    },
  });
}

export function useUnlinkAmenity(roomId: string) {
  const queryClient = useQueryClient();
  const tenantId = useSelector((state: RootState) => state.auth.tenantId);

  return useMutation({
    mutationFn: (amenityId: string) =>
      amenityApi.unlinkAmenityFromRoom(roomId, amenityId, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['amenities', 'room', roomId] });
      toast.success('Amenity unlinked from room');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Failed to unlink amenity from room'
      );
    },
  });
}
