import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { roomsApi, roomTypesApi } from '../api/inventoryApi';
import type { CreateRoomRequest, UpdateRoomRequest } from '../../../types/inventory';
import { useAppSelector } from '../../../redux/hooks';
import { selectTenantId } from '../../../redux/selectors/authSelectors';

// ── Rooms Hooks ───────────────────────────────────────────────────────────────

export function useRooms(tenantId: string | undefined) {
  return useQuery({
    queryKey: ['rooms', tenantId],
    queryFn: () => roomsApi.getRooms(tenantId!),
    enabled: !!tenantId,
  });
}

export function useRoom(id: string | undefined, tenantId: string | undefined) {
  return useQuery({
    queryKey: ['room', id, tenantId],
    queryFn: () => roomsApi.getRoom(id!, tenantId!),
    enabled: !!id && !!tenantId,
  });
}

export function useRoomTypes(roomId: string | undefined, tenantId: string | undefined) {
  return useQuery({
    queryKey: ['room-types', roomId, tenantId],
    queryFn: () => roomsApi.getRoomTypes(roomId!, tenantId!),
    enabled: !!roomId && !!tenantId,
  });
}

export function useAllRoomTypes(tenantId: string | undefined) {
  return useQuery({
    queryKey: ['all-room-types', tenantId],
    queryFn: () => roomTypesApi.getRoomTypes(tenantId!),
    enabled: !!tenantId,
  });
}

// ── Room Mutation Hooks ───────────────────────────────────────────────────────

export function useCreateRoom() {
  const queryClient = useQueryClient();
  const tenantId = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: (req: CreateRoomRequest) => roomsApi.createRoom(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rooms', tenantId] });
      toast.success('Room created successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create room');
    },
  });
}

export function useUpdateRoom() {
  const queryClient = useQueryClient();
  const tenantId = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateRoomRequest }) =>
      roomsApi.updateRoom(id, tenantId!, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rooms', tenantId] });
      toast.success('Room updated successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to update room');
    },
  });
}

export function useDeleteRoom() {
  const queryClient = useQueryClient();
  const tenantId = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: (id: string) => roomsApi.deleteRoom(id, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rooms', tenantId] });
      toast.success('Room deleted successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to delete room');
    },
  });
}

// ── Legacy Hooks (for backward compatibility) ─────────────────────────────────
// These will be removed once all components are updated

export function useResources(tenantId: string | undefined) {
  return useRooms(tenantId);
}

export function useResource(id: string | undefined, tenantId: string | undefined) {
  return useRoom(id, tenantId);
}

export function useResourceServices(roomId: string | undefined, tenantId: string | undefined) {
  return useRoomTypes(roomId, tenantId);
}

export function useServices(tenantId: string | undefined) {
  return useAllRoomTypes(tenantId);
}
