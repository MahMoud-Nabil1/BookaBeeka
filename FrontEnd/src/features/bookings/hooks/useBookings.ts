import { useQuery } from '@tanstack/react-query';
import { bookingApi } from '../api/bookingApi';
import { useAppSelector } from '../../../redux/hooks';
import { selectTenantId, selectUserId } from '../../../redux/selectors/authSelectors';

/**
 * Fetches all bookings for the currently authenticated customer.
 * tenantId is read from the JWT via Redux — customers only see their own bookings.
 * The backend derives customerId from the JWT sub claim automatically.
 */
export function useMyBookings() {
  const tenantId = useAppSelector(selectTenantId);
  const userId = useAppSelector(selectUserId);

  return useQuery({
    queryKey: ['bookings', 'mine', userId, tenantId],
    queryFn: () => bookingApi.getMyBookings(tenantId!),
    enabled: !!tenantId && !!userId,
  });
}
