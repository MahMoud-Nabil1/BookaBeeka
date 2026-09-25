import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { bookingApi } from '../api/bookingApi';
import { paymentApi } from '../../billing/api/paymentApi';
import { useAppSelector } from '../../../redux/hooks';
import { selectUserId } from '../../../redux/selectors/authSelectors';
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
  const navigate = useNavigate();

  return useMutation({
    mutationFn: async (req: CreateBookingRequestDto) => {
      // Step 1 — create booking
      const confirmation = await bookingApi.createBooking(req);

      // Step 2 — wallet checkout
      // The backend's WalletPaymentService.processPayment already confirms the
      // booking internally (PENDING_PAYMENT → CONFIRMED), so no separate Step 3
      // confirm call is needed.
      await paymentApi.checkout({
        bookingId:     confirmation.bookingId,
        customerId:    customerId!,
        tenantId:      req.tenantId,
        paymentAmount: req.paymentAmount && req.paymentAmount > 0 ? req.paymentAmount : 150,
      });

      return confirmation;
    },
    onSuccess: () => {
      // Refresh bookings list and wallet balance
      queryClient.invalidateQueries({ queryKey: ['bookings', 'mine'] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'balance', customerId] });
      queryClient.invalidateQueries({ queryKey: ['wallet', 'history', customerId] });
      toast.success('Room booking confirmed!');
    },
    onError: (error: any) => {
      const status = error?.response?.status;
      if (status === 402) {
        toast.error('Insufficient wallet balance. Please top up your wallet to proceed.', {
          action: {
            label: 'Top Up Wallet',
            onClick: () => navigate('/portal/wallet'),
          },
          duration: 6000,
        });
      } else {
        const msg = error?.response?.data?.message || 'Booking failed. Please try again.';
        toast.error(msg);
      }
    },
  });
}
