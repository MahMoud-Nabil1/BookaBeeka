import { Badge } from '@/components/ui/badge';
import type { BookingStatus } from '../types/booking';

const STATUS_CONFIG: Record<BookingStatus, { label: string; className: string }> = {
  CONFIRMED:       { label: 'Confirmed',       className: 'bg-green-100 text-green-700 hover:bg-green-100 border-green-200' },
  PENDING_PAYMENT: { label: 'Pending Payment', className: 'bg-yellow-100 text-yellow-700 hover:bg-yellow-100 border-yellow-200' },
  COMPLETED:       { label: 'Completed',       className: 'bg-gray-100 text-gray-600 hover:bg-gray-100 border-gray-200' },
  CANCELLED:       { label: 'Cancelled',       className: 'bg-red-100 text-red-700 hover:bg-red-100 border-red-200' },
  EXPIRED:         { label: 'Expired',         className: 'bg-gray-100 text-gray-500 hover:bg-gray-100 border-gray-200' },
};

interface BookingStatusBadgeProps {
  status: BookingStatus;
}

export default function BookingStatusBadge({ status }: BookingStatusBadgeProps) {
  const config = STATUS_CONFIG[status] ?? {
    label: status,
    className: 'bg-muted text-muted-foreground',
  };

  return (
    <Badge variant="outline" className={config.className}>
      {config.label}
    </Badge>
  );
}
