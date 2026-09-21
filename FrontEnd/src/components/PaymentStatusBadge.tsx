import { Badge } from '@/components/ui/badge';

type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
type TransactionType = 'DEPOSIT' | 'PAYMENT' | 'REFUND';

type Status = PaymentStatus | TransactionType;

const STATUS_CONFIG: Record<Status, { label: string; className: string }> = {
  // PaymentStatus
  COMPLETED: { label: 'Completed', className: 'bg-green-100 text-green-700 hover:bg-green-100 border-green-200' },
  PENDING:   { label: 'Pending',   className: 'bg-yellow-100 text-yellow-700 hover:bg-yellow-100 border-yellow-200' },
  FAILED:    { label: 'Failed',    className: 'bg-red-100 text-red-700 hover:bg-red-100 border-red-200' },
  REFUNDED:  { label: 'Refunded',  className: 'bg-blue-100 text-blue-700 hover:bg-blue-100 border-blue-200' },
  // TransactionType
  DEPOSIT:   { label: 'Deposit',   className: 'bg-green-100 text-green-700 hover:bg-green-100 border-green-200' },
  PAYMENT:   { label: 'Payment',   className: 'bg-orange-100 text-orange-700 hover:bg-orange-100 border-orange-200' },
  REFUND:    { label: 'Refund',    className: 'bg-blue-100 text-blue-700 hover:bg-blue-100 border-blue-200' },
};

interface PaymentStatusBadgeProps {
  status: Status;
}

export default function PaymentStatusBadge({ status }: PaymentStatusBadgeProps) {
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
