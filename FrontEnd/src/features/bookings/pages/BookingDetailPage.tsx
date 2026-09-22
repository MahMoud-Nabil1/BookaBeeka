import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { ChevronLeft, CalendarDays, Clock, CreditCard, Loader2, AlertCircle } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { bookingApi } from '../api/bookingApi';
import { useCancelBooking } from '../hooks/useCancelBooking';
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

  // Fetch status (lightweight — has bookingId, status, version)
  const { data: statusData, isLoading, isError } = useQuery({
    queryKey: ['booking', 'status', bookingId, tenantId],
    queryFn: () => bookingApi.getBookingStatus(bookingId!, tenantId!),
    enabled: !!bookingId && !!tenantId,
  });

  const handleCancel = () => {
    cancel(
      { bookingId: bookingId!, reason: reason || undefined },
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

  if (isError || !statusData) {
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

  const canCancel = statusData.status === 'PENDING_PAYMENT' || statusData.status === 'CONFIRMED';

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
              Ref: {statusData.bookingId}
            </p>
          </div>
          <BookingStatusBadge status={statusData.status} />
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
              <span className="font-mono text-foreground">{statusData.bookingId.substring(0, 16)}…</span>
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-2">
                <CreditCard className="h-4 w-4" /> Status
              </span>
              <BookingStatusBadge status={statusData.status} />
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Version</span>
              <span className="text-foreground">{statusData.version}</span>
            </div>
          </CardContent>
        </Card>

        {/* Actions */}
        <div className="flex gap-3">
          <Button variant="outline" asChild className="flex-1">
            <Link to="/portal/rooms">Book Another Room</Link>
          </Button>

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
      </div>
    </PageLayout>
  );
}
