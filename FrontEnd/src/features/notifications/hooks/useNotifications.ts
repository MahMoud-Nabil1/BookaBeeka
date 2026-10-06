import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { notificationApi } from '../api/notificationApi';
import { useAppSelector } from '../../../redux/hooks';
import { selectTenantId, selectUserId } from '../../../redux/selectors/authSelectors';
import type { NotificationDto } from '../../../types/notification';

interface UseMyNotificationsResult {
  data: NotificationDto[] | undefined;
  unreadCount: number;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  refetch: () => void;
}

/**
 * Fetches notifications for the currently authenticated user with automatic polling.
 * Returns notifications and unread count.
 * Polls every 60 seconds to keep notifications up-to-date.
 */
export function useMyNotifications(): UseMyNotificationsResult {
  const tenantId = useAppSelector(selectTenantId);
  const userId = useAppSelector(selectUserId);

  const query = useQuery({
    queryKey: ['notifications', tenantId],
    queryFn: () => notificationApi.getMyNotifications(tenantId || ''),
    enabled: !!userId && !!tenantId,
    refetchInterval: 60_000, // Poll every 60 seconds
    staleTime: 30_000, // Consider data stale after 30 seconds
  });

  const unreadCount = useMemo(() => {
    return query.data?.filter((n) => !n.isRead).length ?? 0;
  }, [query.data]);

  return {
    data: query.data,
    unreadCount,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  };
}

/**
 * Fetches notifications for a specific customer (staff view).
 * Does not include automatic polling.
 */
export function useCustomerNotifications(customerId: string) {
  const tenantId = useAppSelector(selectTenantId);

  return useQuery({
    queryKey: ['notifications', 'customer', customerId, tenantId],
    queryFn: () => notificationApi.getCustomerNotifications(customerId, tenantId || ''),
    enabled: !!customerId && !!tenantId,
  });
}
