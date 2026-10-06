import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { roomBlockApi } from '../api/roomBlockApi';
import type {
  CreateRoomBlockRequest,
  UpdateRoomBlockRequest,
} from '../../../types/availability';

/**
 * Fetch all room blocks for the tenant
 */
export function useRoomBlocks() {
  return useQuery({
    queryKey: ['room-blocks'],
    queryFn: () => roomBlockApi.listRoomBlocks(),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
}

/**
 * Fetch a specific room block by ID
 */
export function useRoomBlock(id: string | undefined) {
  return useQuery({
    queryKey: ['room-blocks', id],
    queryFn: () => roomBlockApi.getRoomBlock(id!),
    enabled: !!id,
  });
}

/**
 * Create a new room block
 */
export function useCreateRoomBlock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateRoomBlockRequest) =>
      roomBlockApi.createRoomBlock(request),
    onSuccess: () => {
      toast.success('Room block created successfully', {
        description: 'The room has been blocked for the specified dates.',
      });
      queryClient.invalidateQueries({ queryKey: ['room-blocks'] });
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        'Failed to create room block.';
      toast.error('Unable to create room block', {
        description: message,
      });
    },
  });
}

/**
 * Update an existing room block
 */
export function useUpdateRoomBlock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: string;
      request: UpdateRoomBlockRequest;
    }) => roomBlockApi.updateRoomBlock(id, request),
    onSuccess: () => {
      toast.success('Room block updated successfully');
      queryClient.invalidateQueries({ queryKey: ['room-blocks'] });
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        'Failed to update room block.';
      toast.error('Unable to update room block', {
        description: message,
      });
    },
  });
}

/**
 * Delete a room block
 */
export function useDeleteRoomBlock() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => roomBlockApi.deleteRoomBlock(id),
    onSuccess: () => {
      toast.success('Room block deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['room-blocks'] });
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        'Failed to delete room block.';
      toast.error('Unable to delete room block', {
        description: message,
      });
    },
  });
}
