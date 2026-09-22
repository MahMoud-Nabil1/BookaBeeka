import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { bookingApi } from '../api/bookingApi';
import { paymentApi } from '../../billing/api/paymentApi';
import { useAppSelector } from '../../../redux/hooks';
import { selectUserId, selectTenantId } from '../../../redux/selectors/authSelectors';
import type { CreateBookingRequestDto } from '../../../types/booking';

/**
 * Orchestrates the 3-step atomic booking flow for hotel rooms:
 *   1. POST /api/bookings             → creates booking (PENDING_PAYMENT) + reserves room slot
 *   2. POST /api/payments/wallet/checkout → deducts wallet, returns PaymentResponse
 *   3. POST /api/bookings/{id}/confirm    → transitions booking to CONFIRMED
 *
 * On any step failure the partial state is left for the backend TTL / lock expiry
 * to clean up (the slot lock has its own expiry window).
 */
export function useCreateBooking() {
  const queryClient = useQueryClient();
  const customerId = useAppSelector(selectUserId);
  const tenantId   = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: async (req: CreateBookingRequestDto) => {
      // Step 1 — create booking
      const confirmation = await bookingApi.createBooking(req);

      // Step 2 — wallet checkout
      await paymentApi.checkout({
        bookingId:     confirmation.bookingId,
        customerId:    customerId!,
        tenantId:      req.tenantId,
        paymentAmount: 0, // amount resolved server-side from the room type price
      });

      // Step 3 — confirm booking
      await bookingApi.confirmBooking(confirmation.bookingId, req.tenantId);

      return confirmation;
    },
    onSuccess: () => {
      // Refresh bookings list and wallet balance
      queryClient.invalidateQueries({ queryKey: ['bookings', 'mine'] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'balance', customerId] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'history', customerId] });
      toast.success('Room booking confirmed!');
    },
    onError: () => {
      toast.error('Booking failed. Please check your wallet balance and try again.');
    },
  });
}
