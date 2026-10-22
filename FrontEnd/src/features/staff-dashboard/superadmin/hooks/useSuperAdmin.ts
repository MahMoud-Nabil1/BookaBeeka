import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { superAdminApi } from '../../api/superAdminApi';

/** Platform-wide KPI stats */
export function usePlatformStats() {
  return useQuery({
    queryKey: ['superadmin', 'stats'],
    queryFn: () => superAdminApi.getPlatformStats(),
  });
}

/** All tenants (first 100) */
export function useTenants(page = 0, size = 100) {
  return useQuery({
    queryKey: ['superadmin', 'tenants', page, size],
    queryFn: () => superAdminApi.listTenants(page, size),
    select: (data) => data.content,
  });
}

/** Paginated tenants */
export function useTenantsPage(page = 0, size = 20) {
  return useQuery({
    queryKey: ['superadmin', 'tenants', 'page', page, size],
    queryFn: () => superAdminApi.listTenants(page, size),
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

/** Suspend a hotel */
export function useSuspendTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tenantId, reason }: { tenantId: string; reason?: string }) =>
      superAdminApi.suspendTenant(tenantId, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'tenants'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'stats'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'rooms'] });
      toast.success(`Hotel "${data.name}" has been suspended`);
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to suspend hotel';
      toast.error(msg);
    },
  });
}

/** Unsuspend a hotel */
export function useUnsuspendTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenantId: string) => superAdminApi.unsuspendTenant(tenantId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'tenants'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'stats'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'rooms'] });
      toast.success(`Hotel "${data.name}" has been unsuspended`);
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to unsuspend hotel';
      toast.error(msg);
    },
  });
}

/** Delete a hotel permanently */
export function useDeleteTenant() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenantId: string) => superAdminApi.deleteTenant(tenantId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'tenants'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'stats'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'rooms'] });
      toast.success('Hotel and associated rooms permanently deleted');
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to delete hotel';
      toast.error(msg);
    },
  });
}

/** All customers */
export function useCustomers(page = 0, size = 50) {
  return useQuery({
    queryKey: ['superadmin', 'customers', page, size],
    queryFn: () => superAdminApi.listCustomers(page, size),
  });
}

/** Ban a customer */
export function useBanCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ customerId, reason }: { customerId: string; reason?: string }) =>
      superAdminApi.banCustomer(customerId, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'customers'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'stats'] });
      toast.success(`Customer "${data.firstName} ${data.lastName}" has been banned`);
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to ban customer';
      toast.error(msg);
    },
  });
}

/** Unban a customer */
export function useUnbanCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (customerId: string) => superAdminApi.unbanCustomer(customerId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'customers'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'stats'] });
      toast.success(`Customer "${data.firstName} ${data.lastName}" has been unbanned`);
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to unban customer';
      toast.error(msg);
    },
  });
}

/** Delete a customer permanently */
export function useDeleteCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (customerId: string) => superAdminApi.deleteCustomer(customerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'customers'] });
      queryClient.invalidateQueries({ queryKey: ['superadmin', 'stats'] });
      toast.success('Customer account deleted successfully');
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to delete customer';
      toast.error(msg);
    },
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
