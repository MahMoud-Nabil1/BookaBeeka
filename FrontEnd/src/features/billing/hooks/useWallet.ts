import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../api/paymentApi';
import type { TopUpRequest } from '../../../types/payment';

export function useWallet(customerId: string | undefined) {
  const queryClient = useQueryClient();

  const balanceQuery = useQuery({
    queryKey: ['wallet', 'balance', customerId],
    queryFn: () => paymentApi.getBalance(customerId!),
    enabled: !!customerId,
  });

  const getHistoryQuery = (page: number, size: number) => useQuery({
    queryKey: ['wallet', 'history', customerId, page, size],
    queryFn: () => paymentApi.getHistory(customerId!, page, size),
    enabled: !!customerId,
  });

  const topUpMutation = useMutation({
    mutationFn: (req: TopUpRequest) => paymentApi.topUpWallet(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet', 'balance', customerId] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'history', customerId] });
    },
  });

  return {
    balanceQuery,
    getHistoryQuery,
    topUpMutation
  };
}
