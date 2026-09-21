import { useQuery } from '@tanstack/react-query';
import { roomsApi, roomTypesApi } from '../api/inventoryApi';

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
