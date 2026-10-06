import api from '../../../config/api';
import type { OwnerLoginRequest, LoginResponse, OwnerRegisterRequest, OwnerRegisterResponse } from '../../../types/auth';

// ── Admin-management types ──────────────────────────────────────────────────

export interface AppointAdminRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
}

export interface AppointAdminResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  message: string;
}

export interface OwnerAdminSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: string;
  tenantId: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

// ── API ─────────────────────────────────────────────────────────────────────

export const ownerApi = {
  ownerLogin: async (req: OwnerLoginRequest): Promise<LoginResponse> => {
    const response = await api.post('/api/auth/owner/login', req);
    return response.data;
  },

  registerOwner: async (req: OwnerRegisterRequest): Promise<OwnerRegisterResponse> => {
    const response = await api.post('/api/v1/owner/register', req);
    return response.data;
  },

  listAdmins: async (): Promise<OwnerAdminSummary[]> => {
    const response = await api.get('/api/v1/owner/admins');
    return response.data;
  },

  appointAdmin: async (req: AppointAdminRequest): Promise<AppointAdminResponse> => {
    const response = await api.post('/api/v1/owner/admins', req);
    return response.data;
  },
};

