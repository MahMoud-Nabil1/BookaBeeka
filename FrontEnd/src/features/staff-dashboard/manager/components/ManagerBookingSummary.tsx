import { CalendarDays, CheckCircle2, Clock } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStaffBookings } from '../../shared/hooks/useStaffBookings';

export default function ManagerBookingSummary() {
  const { data: bookings = [], isLoading } = useStaffBookings();

  const now = new Date();
  const todayStr = now.toDateString();

  const todayCount = bookings.filter(
    (b) => new Date(b.startTime).toDateString() === todayStr
  ).length;
  const confirmedCount  = bookings.filter((b) => b.status === 'CONFIRMED').length;
  const pendingCount    = bookings.filter((b) => b.status === 'PENDING_PAYMENT').length;

  const stats = [
    { label: "Today",     value: todayCount,     icon: CalendarDays },
    { label: "Confirmed", value: confirmedCount,  icon: CheckCircle2 },
    { label: "Pending",   value: pendingCount,    icon: Clock },
  ];

  return (
    <div className="grid grid-cols-3 gap-4">
      {stats.map((s) => (
        <Card key={s.label} className="border-border">
          <CardHeader className="pb-1 flex flex-row items-center justify-between">
            <CardTitle className="text-xs font-medium text-muted-foreground">{s.label}</CardTitle>
            <s.icon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-foreground">
              {isLoading ? '—' : s.value}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
