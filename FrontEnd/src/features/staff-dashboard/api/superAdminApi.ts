import api from '../../../config/api';
import type { SuperAdminLoginRequest, LoginResponse } from '../../../types/auth';
import type {
  PlatformStats,
  TenantSummary,
  TenantDetail,
  CustomerSummary,
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

  // ── Tenants (Hotels) ──────────────────────────────────────────────────────

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

  // PATCH /api/admin/super/tenants/{tenantId}/status
  updateTenantStatus: async (tenantId: string, status: string): Promise<TenantSummary> => {
    const response = await api.patch(`/api/admin/super/tenants/${tenantId}/status`, { status });
    return response.data;
  },

  // POST /api/admin/super/tenants/{tenantId}/suspend
  suspendTenant: async (tenantId: string, reason?: string): Promise<TenantSummary> => {
    const response = await api.post(`/api/admin/super/tenants/${tenantId}/suspend`, { reason });
    return response.data;
  },

  // POST /api/admin/super/tenants/{tenantId}/unsuspend
  unsuspendTenant: async (tenantId: string): Promise<TenantSummary> => {
    const response = await api.post(`/api/admin/super/tenants/${tenantId}/unsuspend`);
    return response.data;
  },

  // DELETE /api/admin/super/tenants/{tenantId}
  deleteTenant: async (tenantId: string): Promise<{ message: string }> => {
    const response = await api.delete(`/api/admin/super/tenants/${tenantId}`);
    return response.data;
  },

  // ── Customers ─────────────────────────────────────────────────────────────

  // GET /api/admin/super/customers?page=&size=
  listCustomers: async (page = 0, size = 50): Promise<PageResponse<CustomerSummary>> => {
    const response = await api.get('/api/admin/super/customers', { params: { page, size } });
    return response.data;
  },

  // POST /api/admin/super/customers/{customerId}/ban
  banCustomer: async (customerId: string, reason?: string): Promise<CustomerSummary> => {
    const response = await api.post(`/api/admin/super/customers/${customerId}/ban`, { reason });
    return response.data;
  },

  // POST /api/admin/super/customers/{customerId}/unban
  unbanCustomer: async (customerId: string): Promise<CustomerSummary> => {
    const response = await api.post(`/api/admin/super/customers/${customerId}/unban`);
    return response.data;
  },

  // DELETE /api/admin/super/customers/{customerId}
  deleteCustomer: async (customerId: string): Promise<{ message: string }> => {
    const response = await api.delete(`/api/admin/super/customers/${customerId}`);
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
