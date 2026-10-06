import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, CalendarDays, Clock, CreditCard, Loader2, AlertCircle, PenLine, BedDouble } from 'lucide-react';
import { format } from 'date-fns';
import { useQuery } from '@tanstack/react-query';
import { bookingApi } from '../api/bookingApi';
import { useCancelBooking } from '../hooks/useCancelBooking';
import { AddReviewModal } from '../../reviews';
import BookingStatusBadge from '../../../components/BookingStatusBadge';
import { useAppSelector } from '../../../redux/hooks';
import { selectTenantId } from '../../../redux/selectors/authSelectors';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import PageLayout from '../../../components/layout/PageLayout';

export default function BookingDetailPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const tenantId = useAppSelector(selectTenantId);
  const { mutate: cancel, isPending: cancelling } = useCancelBooking();
  const navigate = useNavigate();

  const [reason, setReason] = useState('');
  const [open, setOpen] = useState(false);
  const [reviewOpen, setReviewOpen] = useState(false);

  // Fetch full booking details (uses /api/bookings/{bookingId}, with fallback to getMyBookings)
  const { data: booking, isLoading, isError } = useQuery({
    queryKey: ['booking-detail', bookingId, tenantId],
    queryFn: async () => {
      try {
        return await bookingApi.getBooking(bookingId!, tenantId);
      } catch {
        const list = await bookingApi.getMyBookings(tenantId);
        const found = list.find((b) => b.bookingId === bookingId);
        if (!found) throw new Error('Booking not found');
        return found;
      }
    },
    enabled: !!bookingId,
  });

  const now = new Date();
  const stayStarted = booking ? new Date(booking.startTime) < now : false;
  const canReview =
    booking?.status === 'COMPLETED' ||
    (booking?.status === 'CONFIRMED' && stayStarted);

  const handleCancel = () => {
    cancel(
      {
        bookingId: bookingId!,
        tenantId: booking?.tenantId || tenantId || undefined,
        reason: reason || undefined,
      },
      {
        onSuccess: () => {
          setOpen(false);
          navigate('/portal/bookings');
        },
      }
    );
  };

  if (isLoading) {
    return (
      <PageLayout>
        <div className="flex justify-center items-center py-24 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin mr-3" />
          <span>Loading booking...</span>
        </div>
      </PageLayout>
    );
  }

  if (isError || !booking) {
    return (
      <PageLayout>
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <AlertCircle className="h-12 w-12 text-destructive mb-4" />
          <h2 className="text-2xl font-bold mb-2">Booking Not Found</h2>
          <p className="text-muted-foreground mb-6">This booking could not be loaded.</p>
          <Button asChild>
            <Link to="/portal/bookings">Back to Bookings</Link>
          </Button>
        </div>
      </PageLayout>
    );
  }

  const canCancel = booking.status === 'PENDING_PAYMENT' || booking.status === 'CONFIRMED';

  return (
    <PageLayout
      action={
        <Button variant="ghost" asChild className="gap-2">
          <Link to="/portal/bookings">
            <ChevronLeft className="h-4 w-4" /> Back to Bookings
          </Link>
        </Button>
      }
    >
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Booking Details</h1>
            <p className="text-sm text-muted-foreground font-mono mt-1">
              Ref: {booking.bookingId}
            </p>
          </div>
          <BookingStatusBadge status={booking.status} />
        </div>

        {/* Info Card */}
        <Card className="border-border">
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2">
              <CalendarDays className="h-4 w-4 text-muted-foreground" />
              Booking Information
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <Clock className="h-4 w-4" /> Booking ID
              </span>
              <span className="font-mono text-foreground">{booking.bookingId.substring(0, 16)}…</span>
            </div>

            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <BedDouble className="h-4 w-4" /> Room
              </span>
              <span className="text-foreground font-medium text-right">
                {booking.roomName ? (
                  <span>
                    <span>{booking.roomName}</span>
                    {booking.roomNumber && (
                      <span className="text-muted-foreground ml-1.5 font-normal">
                        · Room {booking.roomNumber}
                      </span>
                    )}
                  </span>
                ) : booking.roomNumber ? (
                  <span>Room {booking.roomNumber}</span>
                ) : (
                  <span className="font-mono text-xs text-muted-foreground">
                    {booking.roomId ? `${booking.roomId.substring(0, 8)}…` : '—'}
                  </span>
                )}
              </span>
            </div>

            {booking.roomTypeName && (
              <>
                <Separator />
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">Room Type</span>
                  <span className="text-foreground font-medium">{booking.roomTypeName}</span>
                </div>
              </>
            )}

            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <CalendarDays className="h-4 w-4" /> Check-in
              </span>
              <span className="text-foreground font-medium">
                {booking.checkInDate || (booking.startTime ? format(new Date(booking.startTime), 'EEE, MMM d, yyyy') : '—')}
              </span>
            </div>

            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <CalendarDays className="h-4 w-4" /> Check-out
              </span>
              <span className="text-foreground font-medium">
                {booking.checkOutDate || (booking.endTime ? format(new Date(booking.endTime), 'EEE, MMM d, yyyy') : '—')}
              </span>
            </div>

            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <CreditCard className="h-4 w-4" /> Total Price
              </span>
              <span className="text-foreground font-bold">
                {booking.totalAmount > 0
                  ? `${booking.currency} ${booking.totalAmount.toFixed(2)}`
                  : 'Free'}
              </span>
            </div>

            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <CreditCard className="h-4 w-4" /> Status
              </span>
              <BookingStatusBadge status={booking.status} />
            </div>

            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Version</span>
              <span className="text-foreground">{booking.version}</span>
            </div>
          </CardContent>
        </Card>

        {/* Actions */}
        <div className="flex gap-3 flex-wrap">
          <Button variant="outline" asChild className="flex-1">
            <Link to="/portal/rooms">Book Another Room</Link>
          </Button>

          {canReview && (
            <Button
              className="flex-1 gap-2 shadow-low"
              onClick={() => setReviewOpen(true)}
            >
              <PenLine className="h-4 w-4" />
              Rate Stay
            </Button>
          )}

          {canCancel && (
            <Dialog open={open} onOpenChange={setOpen}>
              <DialogTrigger asChild>
                <Button variant="destructive" className="flex-1">
                  Cancel Booking
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Cancel Booking</DialogTitle>
                  <DialogDescription>
                    Any applicable refund will be returned to your wallet instantly.
                  </DialogDescription>
                </DialogHeader>
                <div className="space-y-2 py-2">
                  <Label htmlFor="reason">Reason (optional)</Label>
                  <Textarea
                    id="reason"
                    placeholder="Let us know why you're cancelling..."
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    rows={3}
                  />
                </div>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setOpen(false)}>Keep Booking</Button>
                  <Button variant="destructive" onClick={handleCancel} disabled={cancelling}>
                    {cancelling && <Loader2 className="h-4 w-4 animate-spin mr-2" />}
                    Confirm Cancel
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          )}
        </div>

        <AddReviewModal
          open={reviewOpen}
          onOpenChange={setReviewOpen}
          serviceId={booking.roomId}
          tenantId={booking.tenantId || tenantId || ''}
          preselectedBookingId={bookingId}
        />
      </div>
    </PageLayout>
  );
}
