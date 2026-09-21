import { Loader2 } from 'lucide-react';
import AdminStatsPanel from '../components/AdminStatsPanel';
import StaffBookingCard from '../../shared/components/StaffBookingCard';
import { useStaffBookings } from '../../shared/hooks/useStaffBookings';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import PageLayout from '../../../../components/layout/PageLayout';

export default function AdminOverviewPage() {
  const { data: bookings = [], isLoading } = useStaffBookings();

  // Show the 5 most recent bookings
  const recent = bookings
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  return (
    <PageLayout title="Overview">
      <div className="space-y-8">
        <AdminStatsPanel />

        <Card className="border-border">
          <CardHeader>
            <CardTitle className="text-base">Recent Bookings</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex items-center gap-2 text-muted-foreground py-6 justify-center">
                <Loader2 className="h-5 w-5 animate-spin" />
                <span className="text-sm">Loading bookings…</span>
              </div>
            ) : recent.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-6">No bookings yet.</p>
            ) : (
              <div className="space-y-3">
                {recent.map((b) => (
                  <StaffBookingCard key={b.bookingId} booking={b} />
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </PageLayout>
  );
}
