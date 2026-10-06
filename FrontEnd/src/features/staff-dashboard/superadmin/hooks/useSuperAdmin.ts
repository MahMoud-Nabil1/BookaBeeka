import { useQuery } from '@tanstack/react-query';
import { superAdminApi } from '../../api/superAdminApi';

/** Platform-wide KPI stats */
export function usePlatformStats() {
  return useQuery({
    queryKey: ['superadmin', 'stats'],
    queryFn: () => superAdminApi.getPlatformStats(),
  });
}

/** All tenants (first 100) */
export function useTenants() {
  return useQuery({
    queryKey: ['superadmin', 'tenants'],
    queryFn: () => superAdminApi.listTenants(0, 100),
    select: (data) => data.content,
  });
}

/** Full detail for a single tenant */
export function useTenantDetail(tenantId: string | null) {
  return useQuery({
    queryKey: ['superadmin', 'tenants', tenantId],
    queryFn: () => superAdminApi.getTenantDetail(tenantId!),
    enabled: !!tenantId,
  });
}

/** Platform-wide transaction feed */
export function usePlatformTransactions(page = 0, size = 20) {
  return useQuery({
    queryKey: ['superadmin', 'transactions', page, size],
    queryFn: () => superAdminApi.listTransactions(page, size),
  });
}

/** All rooms for a specific tenant (used in rooms page) */
export function useTenantRooms(tenantId: string | null) {
  return useQuery({
    queryKey: ['superadmin', 'rooms', tenantId],
    queryFn: () => superAdminApi.getRoomsByTenant(tenantId!),
    enabled: !!tenantId,
  });
}
