import { useQuery } from '@tanstack/react-query';
import { paymentApi } from '../api/paymentApi';

export function useCustomerBalance(customerId: string | undefined) {
  return useQuery({
    queryKey: ['wallet', 'balance', customerId],
    queryFn: () => paymentApi.getBalance(customerId!),
    enabled: !!customerId,
  });
}

export function useCustomerTransactionHistory(
  customerId: string | undefined,
  page = 0,
  size = 10
) {
  return useQuery({
    queryKey: ['wallet', 'history', customerId, page, size],
    queryFn: () => paymentApi.getHistory(customerId!, page, size),
    enabled: !!customerId,
  });
}
