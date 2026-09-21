import { CalendarDays, TrendingUp, CheckCircle2, Clock } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStaffBookings } from '../../shared/hooks/useStaffBookings';
import { useTenantBalance } from '../../shared/hooks/useTenantPayments';

export default function AdminStatsPanel() {
  const { data: bookings = [] } = useStaffBookings();
  const { data: balance } = useTenantBalance();

  const now = new Date();
  const todayStr = now.toDateString();

  const todayBookings  = bookings.filter(b => new Date(b.startTime).toDateString() === todayStr);
  const confirmed      = bookings.filter(b => b.status === 'CONFIRMED').length;
  const pending        = bookings.filter(b => b.status === 'PENDING_PAYMENT').length;
  const totalRevenue   = balance?.balance ?? 0;
  const currency       = balance?.currency ?? 'USD';

  const stats = [
    {
      title: 'Total Revenue',
      value: new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(totalRevenue),
      icon: TrendingUp,
      description: 'Tenant wallet balance',
      color: 'text-green-600',
      bg: 'bg-green-50',
    },
    {
      title: "Today's Bookings",
      value: todayBookings.length.toString(),
      icon: CalendarDays,
      description: 'Bookings starting today',
      color: 'text-blue-600',
      bg: 'bg-blue-50',
    },
    {
      title: 'Confirmed',
      value: confirmed.toString(),
      icon: CheckCircle2,
      description: 'Active confirmed bookings',
      color: 'text-primary',
      bg: 'bg-primary/10',
    },
    {
      title: 'Pending Payment',
      value: pending.toString(),
      icon: Clock,
      description: 'Awaiting payment',
      color: 'text-yellow-600',
      bg: 'bg-yellow-50',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
      {stats.map((stat) => (
        <Card key={stat.title} className="border-border">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">{stat.title}</CardTitle>
            <div className={`p-2 rounded-lg ${stat.bg}`}>
              <stat.icon className={`h-4 w-4 ${stat.color}`} />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-foreground">{stat.value}</div>
            <p className="text-xs text-muted-foreground mt-1">{stat.description}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
