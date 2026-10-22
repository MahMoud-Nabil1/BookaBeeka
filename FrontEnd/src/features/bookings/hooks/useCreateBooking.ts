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
 *   2. POST /api/payments/wallet/checkout → deducts wallet, confirms booking internally
 *   3. On checkout failure: POST /api/bookings/{id}/cancel to roll back
 *
 * The backend's WalletPaymentService.processPayment handles the payment deduction
 * and booking confirmation (PENDING_PAYMENT → CONFIRMED) atomically. If payment
 * fails, we explicitly cancel the booking to release the room slot.
 */
export function useCreateBooking() {
  const queryClient = useQueryClient();
  const customerId = useAppSelector(selectUserId);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: async (req: CreateBookingRequestDto) => {
      let bookingId: string | null = null;

      try {
        // Step 1 — create booking (PENDING_PAYMENT)
        const confirmation = await bookingApi.createBooking(req);
        bookingId = confirmation.bookingId;

        // Step 2 — wallet checkout (deducts wallet + confirms booking internally)
        await paymentApi.checkout({
          bookingId:     confirmation.bookingId,
          customerId:    customerId!,
          tenantId:      req.tenantId,
          paymentAmount: req.paymentAmount && req.paymentAmount > 0 ? req.paymentAmount : 150,
        });

        return confirmation;
      } catch (error) {
        // Step 3 — rollback on checkout failure
        if (bookingId) {
          try {
            await bookingApi.cancelBooking(bookingId, req.tenantId, 'Payment failed');
          } catch (cancelError) {
            console.error('Failed to cancel booking after payment failure:', cancelError);
            // Continue to throw the original payment error
          }
        }
        throw error;
      }
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
      } else if (status === 403) {
        const msg = error?.response?.data?.message || 'Your account is banned from making room bookings.';
        toast.error(msg, { duration: 6000 });
      } else if (status === 409) {
        const msg = error?.response?.data?.message || 'This hotel or room is currently unavailable for booking.';
        toast.error(msg, { duration: 6000 });
      } else {
        const msg = error?.response?.data?.message || 'Booking failed. Please try again.';
        toast.error(msg);
      }
    },
  });
}
