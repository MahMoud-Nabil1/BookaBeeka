import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { bookingApi } from '../api/bookingApi';
import { useAppSelector } from '../../../redux/hooks';
import { selectUserId, selectTenantId } from '../../../redux/selectors/authSelectors';

/**
 * Cancels a booking and triggers a wallet refund automatically on the backend.
 * On success invalidates the bookings list and wallet balance.
 */
export function useCancelBooking() {
  const queryClient  = useQueryClient();
  const customerId   = useAppSelector(selectUserId);
  const tenantId     = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: ({ bookingId, reason }: { bookingId: string; reason?: string }) =>
      bookingApi.cancelBooking(bookingId, tenantId!, reason),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['bookings', 'mine'] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'balance', customerId] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'history', customerId] });

      if (result.refundAmount > 0) {
        toast.success(
          `Booking cancelled. ${result.refundPercentage}% refund ($${result.refundAmount.toFixed(2)}) returned to your wallet.`
        );
      } else {
        toast.success('Booking cancelled.');
      }
    },
    onError: () => {
      toast.error('Failed to cancel booking. Please try again.');
    },
  });
}
