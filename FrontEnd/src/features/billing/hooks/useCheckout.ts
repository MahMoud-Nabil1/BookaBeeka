import { useMutation } from '@tanstack/react-query';
import { paymentApi } from '../api/paymentApi';
import type { PaymentRequest } from '../../../types/payment';

/**
 * Step 2 of the booking flow: deduct the booking amount from the customer wallet.
 * Called after createBooking() succeeds and before confirmBooking().
 */
export function useCheckout() {
  return useMutation({
    mutationFn: (req: PaymentRequest) => paymentApi.checkout(req),
  });
}
