import api from '../../../config/api';
import type {
  AmenityResponse,
  CreateAmenityRequest,
  UpdateAmenityRequest,
} from '../../../types/inventory';

export const amenityApi = {
  // GET /api/inventory/amenities?tenantId=
  listAmenities: async (tenantId: string): Promise<AmenityResponse[]> => {
    const response = await api.get<AmenityResponse[]>('/api/inventory/amenities', {
      params: { tenantId },
    });
    return response.data;
  },

  // GET /api/inventory/amenities/{id}?tenantId=
  getAmenity: async (id: string, tenantId: string): Promise<AmenityResponse> => {
    const response = await api.get<AmenityResponse>(`/api/inventory/amenities/${id}`, {
      params: { tenantId },
    });
    return response.data;
  },

  // POST /api/inventory/amenities
  createAmenity: async (
    req: CreateAmenityRequest,
    tenantId: string
  ): Promise<AmenityResponse> => {
    const response = await api.post<AmenityResponse>('/api/inventory/amenities', req, {
      params: { tenantId },
    });
    return response.data;
  },

  // PUT /api/inventory/amenities/{id}?tenantId=
  updateAmenity: async (
    id: string,
    req: UpdateAmenityRequest,
    tenantId: string
  ): Promise<AmenityResponse> => {
    const response = await api.put<AmenityResponse>(
      `/api/inventory/amenities/${id}`,
      req,
      {
        params: { tenantId },
      }
    );
    return response.data;
  },

  // DELETE /api/inventory/amenities/{id}?tenantId=
  deleteAmenity: async (id: string, tenantId: string): Promise<void> => {
    await api.delete(`/api/inventory/amenities/${id}`, {
      params: { tenantId },
    });
  },

  // POST /api/inventory/resources/{roomId}/amenities/{amenityId}?tenantId=
  linkAmenityToRoom: async (
    roomId: string,
    amenityId: string,
    tenantId: string
  ): Promise<void> => {
    await api.post(
      `/api/inventory/resources/${roomId}/amenities/${amenityId}`,
      null,
      {
        params: { tenantId },
      }
    );
  },

  // DELETE /api/inventory/resources/{roomId}/amenities/{amenityId}?tenantId=
  unlinkAmenityFromRoom: async (
    roomId: string,
    amenityId: string,
    tenantId: string
  ): Promise<void> => {
    await api.delete(`/api/inventory/resources/${roomId}/amenities/${amenityId}`, {
      params: { tenantId },
    });
  },

  // GET /api/inventory/resources/{roomId}/amenities?tenantId=
  listRoomAmenities: async (
    roomId: string,
    tenantId: string
  ): Promise<AmenityResponse[]> => {
    const response = await api.get<AmenityResponse[]>(
      `/api/inventory/resources/${roomId}/amenities`,
      {
        params: { tenantId },
      }
    );
    return response.data;
  },
};
