import { useQuery } from '@tanstack/react-query';
import { availabilityApi } from '../api/availabilityApi';
import type { SlotDto } from '../../../types/availability';

interface UseAvailabilityArgs {
  tenantId: string;
  resourceId: string;
  date: string; // YYYY-MM-DD
}

export const useAvailability = ({ tenantId, resourceId, date }: UseAvailabilityArgs) => {
  return useQuery<SlotDto[]>({
    queryKey: ['availability', 'slots', tenantId, resourceId, date],
    queryFn: () => availabilityApi.getSlots(tenantId, resourceId, date),
    enabled: !!tenantId && !!resourceId && !!date,
  });
};
