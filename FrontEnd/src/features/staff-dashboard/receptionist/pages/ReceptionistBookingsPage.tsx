import { Loader2, CalendarDays } from 'lucide-react';
import ReceptionistCheckInCard from '../components/ReceptionistCheckInCard';
import { useStaffBookings } from '../../shared/hooks/useStaffBookings';
import PageLayout from '../../../../components/layout/PageLayout';

export default function ReceptionistBookingsPage() {
  const { data: bookings = [], isLoading, isError } = useStaffBookings();

  const now = new Date();
  const todayStr = now.toDateString();

  // Show today's and upcoming confirmed/pending bookings, sorted by start time
  const relevant = bookings
    .filter((b) => {
      const start = new Date(b.startTime);
      return (
        (b.status === 'PENDING_PAYMENT' || b.status === 'CONFIRMED') &&
        start >= new Date(now.getFullYear(), now.getMonth(), now.getDate())
      );
    })
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());

  const today    = relevant.filter((b) => new Date(b.startTime).toDateString() === todayStr);
  const upcoming = relevant.filter((b) => new Date(b.startTime).toDateString() !== todayStr);

  return (
    <PageLayout title="Check-In">
      {isLoading && (
        <div className="flex items-center justify-center py-20 text-muted-foreground gap-3">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span>Loading bookings…</span>
        </div>
      )}

      {isError && (
        <p className="text-center text-destructive py-12 text-sm">
          Failed to load bookings. Please refresh.
        </p>
      )}

      {!isLoading && !isError && relevant.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground text-center">
          <CalendarDays className="h-12 w-12 mb-4 opacity-30" />
          <p className="font-medium">No upcoming bookings</p>
          <p className="text-sm mt-1">All clear for today!</p>
        </div>
      )}

      {!isLoading && !isError && (
        <div className="space-y-8">
          {today.length > 0 && (
            <section>
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                Today
              </h2>
              <div className="space-y-3">
                {today.map((b) => (
                  <ReceptionistCheckInCard key={b.bookingId} booking={b} />
                ))}
              </div>
            </section>
          )}

          {upcoming.length > 0 && (
            <section>
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                Upcoming
              </h2>
              <div className="space-y-3">
                {upcoming.map((b) => (
                  <ReceptionistCheckInCard key={b.bookingId} booking={b} />
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </PageLayout>
  );
}
