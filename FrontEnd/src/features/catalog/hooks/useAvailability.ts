import { useQuery } from '@tanstack/react-query';
import { availabilityApi } from '../api/availabilityApi';
import type { SlotDto } from '../../../types/availability';

interface UseAvailabilityArgs {
  tenantId: string;
  branchId: string;
  resourceId: string;
  date: string;
}

export const useAvailability = ({ tenantId, branchId, resourceId, date }: UseAvailabilityArgs) => {
  return useQuery<SlotDto[]>({
    queryKey: ['availability', tenantId, branchId, resourceId, date],
    queryFn: () => availabilityApi.getSlots(tenantId, branchId, resourceId, date),
    enabled: !!tenantId && !!branchId && !!resourceId && !!date, // Only run when all args are present
  });
};
