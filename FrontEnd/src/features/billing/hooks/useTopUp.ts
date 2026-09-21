import { useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../api/paymentApi';
import type { TopUpRequest } from '../../../types/payment';

export function useTopUp(customerId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: TopUpRequest) => paymentApi.topUpWallet(req),
    onSuccess: () => {
      // Invalidate balance and history so they refresh after top-up
      queryClient.invalidateQueries({ queryKey: ['wallet', 'balance', customerId] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'history', customerId] });
    },
  });
}
