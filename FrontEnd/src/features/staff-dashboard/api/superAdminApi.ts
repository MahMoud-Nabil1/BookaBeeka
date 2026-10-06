import api from '../../../config/api';
import type { SuperAdminLoginRequest, LoginResponse } from '../../../types/auth';
import type {
  PlatformStats,
  TenantSummary,
  TenantDetail,
  PlatformTransaction,
  PageResponse,
} from '../../../types/superAdmin';

export const superAdminApi = {
  // ── Auth ─────────────────────────────────────────────────────────────────

  superAdminLogin: async (req: SuperAdminLoginRequest): Promise<LoginResponse> => {
    const response = await api.post('/api/auth/super-admin/login', req);
    return response.data;
  },

  // ── Platform Stats ────────────────────────────────────────────────────────

  // GET /api/admin/super/stats
  getPlatformStats: async (): Promise<PlatformStats> => {
    const response = await api.get('/api/admin/super/stats');
    return response.data;
  },

  // ── Tenants ───────────────────────────────────────────────────────────────

  // GET /api/admin/super/tenants?page=&size=
  listTenants: async (page = 0, size = 100): Promise<PageResponse<TenantSummary>> => {
    const response = await api.get('/api/admin/super/tenants', { params: { page, size } });
    return response.data;
  },

  // GET /api/admin/super/tenants/{tenantId}
  getTenantDetail: async (tenantId: string): Promise<TenantDetail> => {
    const response = await api.get(`/api/admin/super/tenants/${tenantId}`);
    return response.data;
  },

  // ── Transactions ──────────────────────────────────────────────────────────

  // GET /api/admin/super/transactions?page=&size=
  listTransactions: async (page = 0, size = 20): Promise<PageResponse<PlatformTransaction>> => {
    const response = await api.get('/api/admin/super/transactions', { params: { page, size } });
    return response.data;
  },

  // ── Rooms (via dedicated super admin endpoint) ───────────────────────────

  // GET /api/admin/super/tenants/{tenantId}/rooms
  getRoomsByTenant: async (tenantId: string): Promise<any[]> => {
    const response = await api.get(`/api/admin/super/tenants/${tenantId}/rooms`);
    return response.data;
  },
};
