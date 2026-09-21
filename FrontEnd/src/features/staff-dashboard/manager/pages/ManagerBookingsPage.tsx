import { useState } from 'react';
import { Loader2, CalendarDays } from 'lucide-react';
import ManagerBookingSummary from '../components/ManagerBookingSummary';
import StaffBookingCard from '../../shared/components/StaffBookingCard';
import { useStaffBookings } from '../../shared/hooks/useStaffBookings';
import PageLayout from '../../../../components/layout/PageLayout';
import type { BookingStatus } from '../../../../types/booking';

type FilterTab = 'ALL' | BookingStatus;

const TABS: { key: FilterTab; label: string }[] = [
  { key: 'ALL',             label: 'All' },
  { key: 'PENDING_PAYMENT', label: 'Pending' },
  { key: 'CONFIRMED',       label: 'Confirmed' },
  { key: 'COMPLETED',       label: 'Completed' },
  { key: 'CANCELLED',       label: 'Cancelled' },
];

export default function ManagerBookingsPage() {
  const { data: bookings = [], isLoading, isError } = useStaffBookings();
  const [tab, setTab] = useState<FilterTab>('ALL');

  const filtered = tab === 'ALL' ? bookings : bookings.filter((b) => b.status === tab);
  const sorted = filtered
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return (
    <PageLayout title="Bookings">
      <div className="space-y-6">
        <ManagerBookingSummary />

        {/* Filter tabs */}
        <div className="flex gap-2 flex-wrap border-b border-border pb-3">
          {TABS.map((t) => (
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
          <div className="flex items-center justify-center py-16 text-muted-foreground gap-3">
            <Loader2 className="h-6 w-6 animate-spin" />
            <span>Loading bookings…</span>
          </div>
        )}

        {isError && (
          <p className="text-center text-destructive py-10">Failed to load bookings.</p>
        )}

        {!isLoading && !isError && sorted.length === 0 && (
          <div className="flex flex-col items-center py-16 text-muted-foreground text-center">
            <CalendarDays className="h-10 w-10 mb-3 opacity-30" />
            <p>No bookings found.</p>
          </div>
        )}

        {!isLoading && !isError && sorted.length > 0 && (
          <div className="space-y-3">
            {sorted.map((b) => (
              <StaffBookingCard key={b.bookingId} booking={b} canCancel={true} />
            ))}
          </div>
        )}
      </div>
    </PageLayout>
  );
}
