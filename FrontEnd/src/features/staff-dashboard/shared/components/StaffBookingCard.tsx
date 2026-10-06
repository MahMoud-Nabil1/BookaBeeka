import { useState } from 'react';
import { format } from 'date-fns';
import { CalendarDays, Clock, Loader2, CheckCircle2 } from 'lucide-react';
import BookingStatusBadge from '../../../../components/BookingStatusBadge';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { useConfirmBooking, useCancelBookingStaff, useCompleteBooking } from '../hooks/useStaffBookings';
import type { BookingDto } from '../../../../types/booking';

interface StaffBookingCardProps {
  booking: BookingDto;
  /** If false, hide the Cancel button (e.g. for receptionist role) */
  canCancel?: boolean;
  /** If false, hide the Complete button (e.g. for receptionist role) */
  canComplete?: boolean;
}

export default function StaffBookingCard({ booking, canCancel = true, canComplete = true }: StaffBookingCardProps) {
  const { mutate: confirm, isPending: confirming } = useConfirmBooking();
  const { mutate: cancel, isPending: cancelling } = useCancelBookingStaff();
  const { mutate: complete, isPending: completing } = useCompleteBooking();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [reason, setReason] = useState('');

  const canConfirm = booking.status === 'PENDING_PAYMENT';
  const canCancelBooking = canCancel && (booking.status === 'PENDING_PAYMENT' || booking.status === 'CONFIRMED');
  const canCompleteBooking = canComplete && booking.status === 'CONFIRMED';

  return (
    <Card className="border-border hover:shadow-low transition-shadow">
      <CardContent className="p-4 flex items-start justify-between gap-4 flex-wrap">
        <div className="space-y-1.5 flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <BookingStatusBadge status={booking.status} />
            <span className="text-xs text-muted-foreground font-mono">
              #{booking.bookingId.substring(0, 8)}
            </span>
            {(booking.roomName || booking.roomNumber) && (
              <span className="text-xs font-medium text-foreground bg-muted px-2 py-0.5 rounded">
                {booking.roomName || ''}{booking.roomNumber ? (booking.roomName ? ` · Room ${booking.roomNumber}` : `Room ${booking.roomNumber}`) : ''}
              </span>
            )}
          </div>

          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <CalendarDays className="h-3.5 w-3.5 shrink-0" />
            <span>{format(new Date(booking.startTime), 'EEE, MMM d yyyy')}</span>
          </div>

          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Clock className="h-3.5 w-3.5 shrink-0" />
            <span>
              {format(new Date(booking.startTime), 'h:mm a')} – {format(new Date(booking.endTime), 'h:mm a')}
            </span>
          </div>

          {booking.totalAmount > 0 && (
            <p className="text-sm font-semibold text-primary">
              {booking.currency} {booking.totalAmount.toFixed(2)}
            </p>
          )}
        </div>

        <div className="flex items-center gap-2 shrink-0 flex-wrap justify-end">
          {canConfirm && (
            <Button
              size="sm"
              onClick={() => confirm(booking.bookingId)}
              disabled={confirming}
            >
              {confirming && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />}
              Confirm
            </Button>
          )}

          {canCompleteBooking && (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button size="sm" variant="default" className="bg-green-600 hover:bg-green-700 text-white">
                  <CheckCircle2 className="h-3.5 w-3.5 mr-1.5" />
                  Complete
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Complete Booking</AlertDialogTitle>
                  <AlertDialogDescription>
                    Mark this booking as completed? This confirms the customer has checked out and allows them to leave a review.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    disabled={completing}
                    onClick={(e) => {
                      e.preventDefault();
                      complete(booking.bookingId);
                    }}
                    className="bg-green-600 hover:bg-green-700"
                  >
                    {completing && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />}
                    Complete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}

          {canCancelBooking && (
            <Dialog open={cancelOpen} onOpenChange={setCancelOpen}>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" className="text-destructive border-destructive/30 hover:bg-destructive/5">
                  Cancel
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Cancel Booking</DialogTitle>
                  <DialogDescription>
                    This will cancel the booking and trigger a wallet refund if applicable.
                  </DialogDescription>
                </DialogHeader>
                <div className="space-y-2 py-2">
                  <Label htmlFor="staff-cancel-reason">Reason (optional)</Label>
                  <Textarea
                    id="staff-cancel-reason"
                    placeholder="Reason for cancellation..."
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    rows={3}
                  />
                </div>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setCancelOpen(false)}>Keep</Button>
                  <Button
                    variant="destructive"
                    disabled={cancelling}
                    onClick={() =>
                      cancel(
                        { bookingId: booking.bookingId, reason: reason || undefined },
                        { onSuccess: () => setCancelOpen(false) }
                      )
                    }
                  >
                    {cancelling && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />}
                    Confirm Cancel
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
