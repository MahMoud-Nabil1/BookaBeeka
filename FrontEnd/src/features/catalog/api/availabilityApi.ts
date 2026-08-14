import api from '../../../config/api';
import type { SlotDto } from '../../../types/availability';

// Since the backend doesn't have a full resource catalog endpoint yet, 
// we use mock data for the resources, but we will fetch slots dynamically.

export const availabilityApi = {
  getSlots: async (tenantId: string, branchId: string, resourceId: string, date: string): Promise<SlotDto[]> => {
    const response = await api.get('/availability/slots', {
      params: { tenantId, branchId, resourceId, date }
    });
    return response.data;
  }
};
