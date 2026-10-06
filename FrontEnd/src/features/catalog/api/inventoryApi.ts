import api from '../../../config/api';
import type {
  RoomResponse,
  CreateRoomRequest,
  UpdateRoomRequest,
  RoomTypeResponse,
  CreateRoomTypeRequest,
  UpdateRoomTypeRequest,
} from '../../../types/inventory';

// ── API Adapters ──────────────────────────────────────────────────────────────
// These adapters map between frontend hotel terminology and backend generic API

type BackendResourceResponse = {
  id: string;
  tenantId: string;
  branchId: string;
  name: string;
  resourceType: string;
  capacity: number;
  specs: Record<string, unknown> | null;
  isActive: boolean;
  isBookable: boolean;
  createdAt: string;
};

type BackendServiceResponse = {
  id: string;
  tenantId: string;
  branchId: string;
  name: string;
  price: number;
  durationMinutes: number;
  bufferMinutes: number;
  customAttributes: Record<string, unknown> | null;
  isActive: boolean;
  createdAt: string;
};

function mapToRoomResponse(backend: BackendResourceResponse): RoomResponse {
  return {
    id: backend.id,
    tenantId: backend.tenantId,
    branchId: backend.branchId,
    name: backend.name,
    roomCategory: backend.resourceType,
    capacity: backend.capacity,
    specs: backend.specs,
    isActive: backend.isActive,
    isBookable: backend.isBookable,
    createdAt: backend.createdAt,
  };
}

function mapToRoomTypeResponse(backend: BackendServiceResponse): RoomTypeResponse {
  return {
    id: backend.id,
    tenantId: backend.tenantId,
    branchId: backend.branchId,
    name: backend.name,
    price: backend.price,
    durationMinutes: backend.durationMinutes,
    bufferMinutes: backend.bufferMinutes,
    customAttributes: backend.customAttributes,
    isActive: backend.isActive,
    createdAt: backend.createdAt,
  };
}

function mapToBackendResourceRequest(room: CreateRoomRequest | UpdateRoomRequest) {
  const result: Record<string, unknown> = { ...room };
  if ('roomCategory' in room) {
    result.resourceType = room.roomCategory;
    delete result.roomCategory;
  }
  return result;
}

function mapToBackendServiceRequest(roomType: CreateRoomTypeRequest | UpdateRoomTypeRequest) {
  return roomType; // No field name changes needed
}

// ── Rooms API ─────────────────────────────────────────────────────────────────
// Frontend uses "rooms", backend API uses "/api/inventory/resources"

export const roomsApi = {
  // GET /api/inventory/resources?tenantId=
  getRooms: async (tenantId: string): Promise<RoomResponse[]> => {
    const response = await api.get<BackendResourceResponse[]>('/api/inventory/resources', {
      params: { tenantId },
    });
    return response.data.map(mapToRoomResponse);
  },

  // GET /api/inventory/resources/{id}?tenantId=
  getRoom: async (id: string, tenantId: string): Promise<RoomResponse> => {
    const response = await api.get<BackendResourceResponse>(`/api/inventory/resources/${id}`, {
      params: { tenantId },
    });
    return mapToRoomResponse(response.data);
  },

  // POST /api/inventory/resources
  createRoom: async (req: CreateRoomRequest): Promise<RoomResponse> => {
    const backendReq = mapToBackendResourceRequest(req);
    const response = await api.post<BackendResourceResponse>('/api/inventory/resources', backendReq);
    return mapToRoomResponse(response.data);
  },

  // PUT /api/inventory/resources/{id}?tenantId=
  updateRoom: async (
    id: string,
    tenantId: string,
    req: UpdateRoomRequest
  ): Promise<RoomResponse> => {
    const backendReq = mapToBackendResourceRequest(req);
    const response = await api.put<BackendResourceResponse>(`/api/inventory/resources/${id}`, backendReq, {
      params: { tenantId },
    });
    return mapToRoomResponse(response.data);
  },

  // DELETE /api/inventory/resources/{id}?tenantId=
  deleteRoom: async (id: string, tenantId: string): Promise<void> => {
    await api.delete(`/api/inventory/resources/${id}`, { params: { tenantId } });
  },

  // GET /api/inventory/resources/{roomId}/services?tenantId=
  // Returns available room types for this specific room
  getRoomTypes: async (
    roomId: string,
    tenantId: string
  ): Promise<RoomTypeResponse[]> => {
    const response = await api.get<BackendServiceResponse[]>(`/api/inventory/resources/${roomId}/services`, {
      params: { tenantId },
    });
    return response.data.map(mapToRoomTypeResponse);
  },

  // POST /api/inventory/resources/{roomId}/link-service/{serviceOfferingId}?tenantId=
  linkRoomType: async (roomId: string, tenantId: string, roomTypeId: string) => {
    await api.post(`/api/inventory/resources/${roomId}/link-service/${roomTypeId}`, null, {
      params: { tenantId },
    });
  },

  // DELETE /api/inventory/resources/{roomId}/unlink-service/{serviceOfferingId}?tenantId=
  unlinkRoomType: async (roomId: string, tenantId: string, roomTypeId: string) => {
    await api.delete(`/api/inventory/resources/${roomId}/unlink-service/${roomTypeId}`, {
      params: { tenantId },
    });
  },
};

// ── Room Types API ────────────────────────────────────────────────────────────
// Frontend uses "room types", backend API uses "/api/inventory/services"

export const roomTypesApi = {
  // GET /api/inventory/services?tenantId=
  getRoomTypes: async (tenantId: string): Promise<RoomTypeResponse[]> => {
    const response = await api.get<BackendServiceResponse[]>('/api/inventory/services', {
      params: { tenantId },
    });
    return response.data.map(mapToRoomTypeResponse);
  },

  // GET /api/inventory/services/{id}?tenantId=
  getRoomType: async (id: string, tenantId: string): Promise<RoomTypeResponse> => {
    const response = await api.get<BackendServiceResponse>(`/api/inventory/services/${id}`, {
      params: { tenantId },
    });
    return mapToRoomTypeResponse(response.data);
  },

  // POST /api/inventory/services
  createRoomType: async (
    req: CreateRoomTypeRequest
  ): Promise<RoomTypeResponse> => {
    const backendReq = mapToBackendServiceRequest(req);
    const response = await api.post<BackendServiceResponse>('/api/inventory/services', backendReq);
    return mapToRoomTypeResponse(response.data);
  },

  // PUT /api/inventory/services/{id}?tenantId=
  updateRoomType: async (
    id: string,
    tenantId: string,
    req: UpdateRoomTypeRequest
  ): Promise<RoomTypeResponse> => {
    const backendReq = mapToBackendServiceRequest(req);
    const response = await api.put<BackendServiceResponse>(`/api/inventory/services/${id}`, backendReq, {
      params: { tenantId },
    });
    return mapToRoomTypeResponse(response.data);
  },

  // DELETE /api/inventory/services/{id}?tenantId=
  deleteRoomType: async (id: string, tenantId: string): Promise<void> => {
    await api.delete(`/api/inventory/services/${id}`, { params: { tenantId } });
  },
};

// Legacy exports for backward compatibility during refactor
export const inventoryApi = {
  ...roomsApi,
  ...roomTypesApi,
};
