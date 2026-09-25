import api from '../../../config/api';
import type { StaffLoginRequest, LoginResponse } from '../../../types/auth';

export const staffApi = {
  staffLogin: async (req: StaffLoginRequest): Promise<LoginResponse> => {
    const response = await api.post('/api/auth/staff/login', req);
    return response.data;
  }
};

