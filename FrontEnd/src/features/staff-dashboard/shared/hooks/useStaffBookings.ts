import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { staffBookingApi } from '../../api/staffBookingApi';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';

/**
 * Fetches all bookings visible to the authenticated staff member's tenant.
 * tenantId is pulled from the JWT via Redux.
 */
export function useStaffBookings() {
  const tenantId = useAppSelector(selectTenantId);

  return useQuery({
    queryKey: ['staff', 'bookings', tenantId],
    queryFn: () => staffBookingApi.getTenantBookings(tenantId!),
    enabled: !!tenantId,
  });
}

/** Confirm a specific booking (staff action). */
export function useConfirmBooking() {
  const queryClient = useQueryClient();
  const tenantId    = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: (bookingId: string) =>
      staffBookingApi.confirmBooking(bookingId, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff', 'bookings', tenantId] });
      toast.success('Booking confirmed.');
    },
    onError: () => toast.error('Failed to confirm booking.'),
  });
}

/** Cancel a specific booking (staff action). */
export function useCancelBookingStaff() {
  const queryClient = useQueryClient();
  const tenantId    = useAppSelector(selectTenantId);

  return useMutation({
    mutationFn: ({ bookingId, reason }: { bookingId: string; reason?: string }) =>
      staffBookingApi.cancelBooking(bookingId, tenantId!, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff', 'bookings', tenantId] });
      toast.success('Booking cancelled.');
    },
    onError: () => toast.error('Failed to cancel booking.'),
  });
}
