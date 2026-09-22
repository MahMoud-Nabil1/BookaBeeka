import { useQuery } from '@tanstack/react-query';
import { paymentApi } from '../../../billing/api/paymentApi';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';

/**
 * Fetches the tenant wallet balance.
 * Used by AdminPaymentsPage and the StaffPaymentsPage.
 */
export function useTenantBalance() {
  const tenantId = useAppSelector(selectTenantId);

  return useQuery({
    queryKey: ['tenant', 'balance', tenantId],
    queryFn: () => paymentApi.getTenantBalance(tenantId!),
    enabled: !!tenantId,
  });
}

/**
 * Fetches paginated payment history for the current tenant.
 */
export function useTenantPaymentHistory(page = 0, size = 10) {
  const tenantId = useAppSelector(selectTenantId);

  return useQuery({
    queryKey: ['tenant', 'payments', tenantId, page, size],
    queryFn: () => paymentApi.getTenantHistory(tenantId!, page, size),
    enabled: !!tenantId,
  });
}
