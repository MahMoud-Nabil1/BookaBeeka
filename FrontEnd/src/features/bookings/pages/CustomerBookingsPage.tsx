import { useState } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { CalendarDays, Loader2, PackageSearch, ChevronRight, PenLine } from 'lucide-react';
import { useMyBookings } from '../hooks/useBookings';
import { useCancelBooking } from '../hooks/useCancelBooking';
import { AddReviewModal } from '../../reviews';
import BookingStatusBadge from '../../../components/BookingStatusBadge';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import PageLayout from '../../../components/layout/PageLayout';
import type { BookingDto } from '../../../types/booking';

const CANCELLABLE_STATUSES = ['PENDING_PAYMENT', 'CONFIRMED'] as const;

function BookingRow({ booking }: { booking: BookingDto }) {
  const { mutate: cancel, isPending } = useCancelBooking();
  const [reason, setReason] = useState('');
  const [open, setOpen] = useState(false);
  const [reviewOpen, setReviewOpen] = useState(false);

  const canCancel = (CANCELLABLE_STATUSES as readonly string[]).includes(booking.status);

  const handleCancel = () => {
    cancel(
      { bookingId: booking.bookingId, tenantId: booking.tenantId, reason: reason || undefined },
      { onSuccess: () => setOpen(false) }
    );
  };

  return (
    <Card className="border-border hover:shadow-low transition-shadow">
      <CardContent className="p-5">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div className="space-y-1 flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <BookingStatusBadge status={booking.status} />
              <span className="text-xs text-muted-foreground font-mono">
                #{booking.bookingId.substring(0, 8)}
              </span>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground mt-2">
              <CalendarDays className="h-4 w-4 shrink-0" />
              <span>
                {format(new Date(booking.startTime), 'EEE, MMM d yyyy')} ·{' '}
                {format(new Date(booking.startTime), 'h:mm a')} –{' '}
                {format(new Date(booking.endTime), 'h:mm a')}
              </span>
            </div>
            <div className="text-sm font-semibold text-primary mt-1">
              {booking.totalAmount > 0
                ? `${booking.currency} ${booking.totalAmount.toFixed(2)}`
                : 'Free'}
            </div>
            {booking.cancellationReason && (
              <p className="text-xs text-muted-foreground mt-1 italic">
                Reason: {booking.cancellationReason}
              </p>
            )}
          </div>

          <div className="flex items-center gap-2 shrink-0 flex-wrap">
            <Button variant="ghost" size="sm" asChild>
              <Link to={`/portal/bookings/${booking.bookingId}`}>
                Details <ChevronRight className="ml-1 h-4 w-4" />
              </Link>
            </Button>

            {booking.status === 'COMPLETED' && (
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 border-primary/40 text-primary hover:bg-primary/5 shadow-none"
                onClick={() => setReviewOpen(true)}
              >
                <PenLine className="h-3.5 w-3.5" />
                Review Stay
              </Button>
            )}

            <AddReviewModal
              open={reviewOpen}
              onOpenChange={setReviewOpen}
              serviceId={booking.roomId}
              tenantId={booking.tenantId}
              preselectedBookingId={booking.bookingId}
            />

            {canCancel && (
              <Dialog open={open} onOpenChange={setOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline" size="sm" className="text-destructive border-destructive/30 hover:bg-destructive/5">
                    Cancel
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-md">
                  <DialogHeader>
                    <DialogTitle>Cancel Booking</DialogTitle>
                    <DialogDescription>
                      Are you sure you want to cancel this booking? Any applicable refund will be
                      returned to your wallet instantly.
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
                    <Button variant="destructive" onClick={handleCancel} disabled={isPending}>
                      {isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                      Confirm Cancel
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

type FilterTab = 'all' | 'upcoming' | 'past';

export default function CustomerBookingsPage() {
  const { data: bookings, isLoading, isError } = useMyBookings();
  const [tab, setTab] = useState<FilterTab>('all');

  const now = new Date();

  const filtered = (bookings ?? []).filter((b) => {
    if (tab === 'upcoming') return new Date(b.startTime) >= now && b.status !== 'CANCELLED' && b.status !== 'EXPIRED';
    if (tab === 'past')     return new Date(b.endTime) < now || b.status === 'COMPLETED' || b.status === 'CANCELLED';
    return true;
  });

  const tabs: { key: FilterTab; label: string }[] = [
    { key: 'all',      label: 'All' },
    { key: 'upcoming', label: 'Upcoming' },
    { key: 'past',     label: 'Past' },
  ];

  return (
    <PageLayout title="My Bookings">
      {/* Filter tabs */}
      <div className="flex gap-2 mb-6 border-b border-border pb-2">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
              tab === t.key
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:text-foreground hover:bg-muted'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {isLoading && (
        <div className="flex justify-center items-center py-20 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin mr-3" />
          <span>Loading your bookings...</span>
        </div>
      )}

      {isError && (
        <div className="flex flex-col items-center justify-center py-20 text-center text-muted-foreground">
          <PackageSearch className="h-12 w-12 mb-4 opacity-40" />
          <p className="text-lg font-medium">Could not load bookings</p>
          <p className="text-sm mt-1">Please try refreshing the page.</p>
        </div>
      )}

      {!isLoading && !isError && filtered.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center text-muted-foreground">
          <CalendarDays className="h-12 w-12 mb-4 opacity-40" />
          <p className="text-lg font-medium">No bookings found</p>
          {tab === 'all' && (
            <>
              <p className="text-sm mt-1">You haven't made any bookings yet.</p>
              <Button asChild className="mt-6">
                <Link to="/portal/rooms">Browse Rooms</Link>
              </Button>
            </>
          )}
        </div>
      )}

      {!isLoading && !isError && filtered.length > 0 && (
        <div className="space-y-3">
          {filtered
            .slice()
            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
            .map((booking) => (
              <BookingRow key={booking.bookingId} booking={booking} />
            ))}
        </div>
      )}
    </PageLayout>
  );
}
