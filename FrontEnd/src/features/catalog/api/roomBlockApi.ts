import api from '../../../config/api';
import type {
  RoomBlockResponse,
  CreateRoomBlockRequest,
  UpdateRoomBlockRequest,
} from '../../../types/availability';

const ROOM_BLOCKS_BASE = '/api/availability/room-blocks';

export const roomBlockApi = {
  /**
   * Create a new room block
   */
  createRoomBlock: async (
    request: CreateRoomBlockRequest
  ): Promise<RoomBlockResponse> => {
    const { data } = await api.post<RoomBlockResponse>(
      ROOM_BLOCKS_BASE,
      request
    );
    return data;
  },

  /**
   * List all room blocks for the tenant
   */
  listRoomBlocks: async (): Promise<RoomBlockResponse[]> => {
    const { data } = await api.get<RoomBlockResponse[]>(ROOM_BLOCKS_BASE);
    return data;
  },

  /**
   * Get a specific room block by ID
   */
  getRoomBlock: async (id: string): Promise<RoomBlockResponse> => {
    const { data } = await api.get<RoomBlockResponse>(
      `${ROOM_BLOCKS_BASE}/${id}`
    );
    return data;
  },

  /**
   * Update an existing room block
   */
  updateRoomBlock: async (
    id: string,
    request: UpdateRoomBlockRequest
  ): Promise<RoomBlockResponse> => {
    const { data } = await api.put<RoomBlockResponse>(
      `${ROOM_BLOCKS_BASE}/${id}`,
      request
    );
    return data;
  },

  /**
   * Delete a room block
   */
  deleteRoomBlock: async (id: string): Promise<void> => {
    await api.delete(`${ROOM_BLOCKS_BASE}/${id}`);
  },
};
