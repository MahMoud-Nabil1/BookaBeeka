import { format } from 'date-fns';
import PaymentStatusBadge from '../../../../components/PaymentStatusBadge';
import { TableCell, TableRow } from '@/components/ui/table';
import type { TenantPaymentDetail } from '../../../../types/payment';

interface TenantTransactionRowProps {
  payment: TenantPaymentDetail;
}

export default function TenantTransactionRow({ payment }: TenantTransactionRowProps) {
  return (
    <TableRow>
      <TableCell className="whitespace-nowrap">
        <div className="font-medium text-foreground text-sm">
          {format(new Date(payment.createdAt), 'MMM d, yyyy')}
        </div>
        <div className="text-xs text-muted-foreground">
          {format(new Date(payment.createdAt), 'h:mm a')}
        </div>
      </TableCell>
      <TableCell>
        <span className="font-mono text-xs text-muted-foreground">
          {payment.bookingId.substring(0, 8)}…
        </span>
      </TableCell>
      <TableCell>
        <PaymentStatusBadge status={payment.status} />
      </TableCell>
      <TableCell className="text-sm text-muted-foreground">{payment.paymentMethod}</TableCell>
      <TableCell className="text-right font-semibold text-foreground">
        {new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: payment.currency,
        }).format(payment.amount)}
      </TableCell>
    </TableRow>
  );
}
