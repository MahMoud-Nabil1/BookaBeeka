import { format } from 'date-fns';
import { Clock, CheckCircle2, Loader2 } from 'lucide-react';
import BookingStatusBadge from '../../../../components/BookingStatusBadge';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useConfirmBooking } from '../../shared/hooks/useStaffBookings';
import type { BookingDto } from '../../../../types/booking';

interface ReceptionistCheckInCardProps {
  booking: BookingDto;
}

export default function ReceptionistCheckInCard({ booking }: ReceptionistCheckInCardProps) {
  const { mutate: confirm, isPending } = useConfirmBooking();

  const canCheckIn = booking.status === 'PENDING_PAYMENT';

  return (
    <Card className="border-border hover:shadow-low transition-shadow">
      <CardContent className="p-4 flex items-center justify-between gap-4 flex-wrap">
        <div className="space-y-1.5 flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <BookingStatusBadge status={booking.status} />
            <span className="text-xs font-mono text-muted-foreground">
              #{booking.bookingId.substring(0, 8)}
            </span>
          </div>

          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Clock className="h-3.5 w-3.5 shrink-0" />
            <span>
              {format(new Date(booking.startTime), 'h:mm a')} –{' '}
              {format(new Date(booking.endTime), 'h:mm a')}
            </span>
            <span className="text-xs">
              {format(new Date(booking.startTime), 'EEE, MMM d')}
            </span>
          </div>

          {booking.totalAmount > 0 && (
            <p className="text-sm font-semibold text-primary">
              {booking.currency} {booking.totalAmount.toFixed(2)}
            </p>
          )}
        </div>

        {canCheckIn ? (
          <Button
            size="sm"
            onClick={() => confirm(booking.bookingId)}
            disabled={isPending}
            className="shrink-0"
          >
            {isPending
              ? <><Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" /> Confirming…</>
              : <><CheckCircle2 className="h-3.5 w-3.5 mr-1.5" /> Check In</>
            }
          </Button>
        ) : (
          <span className="text-xs text-muted-foreground shrink-0 italic">
            {booking.status === 'CONFIRMED' ? 'Checked in' : booking.status.toLowerCase()}
          </span>
        )}
      </CardContent>
    </Card>
  );
}
