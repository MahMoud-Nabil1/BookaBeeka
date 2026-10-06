import {
  Building2, Users, CalendarDays, TrendingUp,
  CheckCircle2, XCircle, Loader2, AlertTriangle,
  DollarSign, Clock,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import PageLayout from '../../../../components/layout/PageLayout';
import { usePlatformStats } from '../hooks/useSuperAdmin';

function StatCard({
  title,
  value,
  description,
  icon: Icon,
  color,
  bg,
}: {
  title: string;
  value: string;
  description: string;
  icon: React.ElementType;
  color: string;
  bg: string;
}) {
  return (
    <Card className="border-border">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <div className={`p-2 rounded-lg ${bg}`}>
          <Icon className={`h-4 w-4 ${color}`} />
        </div>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold text-foreground">{value}</div>
        <p className="text-xs text-muted-foreground mt-1">{description}</p>
      </CardContent>
    </Card>
  );
}

export default function SuperAdminOverviewPage() {
  const { data: stats, isLoading, isError } = usePlatformStats();

  if (isLoading) {
    return (
      <PageLayout title="Platform Overview">
        <div className="flex items-center justify-center py-24 text-muted-foreground gap-3">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span>Loading platform stats…</span>
        </div>
      </PageLayout>
    );
  }

  if (isError || !stats) {
    return (
      <PageLayout title="Platform Overview">
        <div className="flex flex-col items-center justify-center py-24 text-destructive gap-3">
          <AlertTriangle className="h-8 w-8" />
          <p>Failed to load platform stats. Please refresh.</p>
        </div>
      </PageLayout>
    );
  }

  const currency = 'USD';
  const fmt = (n: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(n);

  const topStats = [
    {
      title: 'Platform Revenue',
      value: fmt(stats.platformRevenue),
      description: 'Total money earned across all tenants',
      icon: TrendingUp,
      color: 'text-green-600',
      bg: 'bg-green-50 dark:bg-green-950',
    },
    {
      title: 'Money in Circulation',
      value: fmt(stats.moneyInCirculation),
      description: 'Total wallet balances (customers + tenants)',
      icon: DollarSign,
      color: 'text-blue-600',
      bg: 'bg-blue-50 dark:bg-blue-950',
    },
    {
      title: 'Active Tenants',
      value: `${stats.activeTenants}`,
      description: `${stats.totalTenants} total (${stats.suspendedTenants} suspended, ${stats.bannedTenants} banned)`,
      icon: Building2,
      color: 'text-primary',
      bg: 'bg-primary/10',
    },
    {
      title: 'Total Customers',
      value: `${stats.totalCustomers}`,
      description: `${stats.bannedCustomers} banned`,
      icon: Users,
      color: 'text-violet-600',
      bg: 'bg-violet-50 dark:bg-violet-950',
    },
  ];

  const bookingStats = [
    {
      title: 'Total Bookings',
      value: `${stats.totalBookings}`,
      description: 'All time across the platform',
      icon: CalendarDays,
      color: 'text-blue-600',
      bg: 'bg-blue-50 dark:bg-blue-950',
    },
    {
      title: 'Confirmed',
      value: `${stats.confirmedBookings}`,
      description: 'Currently active bookings',
      icon: CheckCircle2,
      color: 'text-green-600',
      bg: 'bg-green-50 dark:bg-green-950',
    },
    {
      title: 'Completed',
      value: `${stats.completedBookings}`,
      description: 'Successfully checked out',
      icon: CheckCircle2,
      color: 'text-primary',
      bg: 'bg-primary/10',
    },
    {
      title: 'Cancelled',
      value: `${stats.cancelledBookings}`,
      description: 'Cancelled by customer or staff',
      icon: XCircle,
      color: 'text-destructive',
      bg: 'bg-destructive/10',
    },
  ];

  const alertStats = [
    {
      title: 'Stuck Bookings',
      value: `${stats.stuckBookings}`,
      description: 'Pending > 30 min — may need attention',
      icon: Clock,
      color: 'text-yellow-600',
      bg: 'bg-yellow-50 dark:bg-yellow-950',
    },
    {
      title: 'Failed Payments',
      value: `${stats.failedPaymentsCount}`,
      description: 'Payment attempts that did not complete',
      icon: AlertTriangle,
      color: 'text-destructive',
      bg: 'bg-destructive/10',
    },
    {
      title: 'Hotel Staff Users',
      value: `${stats.totalHotelUsers}`,
      description: 'Owners, admins, and receptionists',
      icon: Users,
      color: 'text-muted-foreground',
      bg: 'bg-muted',
    },
  ];

  return (
    <PageLayout
      title="Platform Overview"
      description="Real-time metrics across all tenants on the platform."
    >
      <div className="space-y-8">
        {/* Top financials + tenant row */}
        <div>
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
            Financials &amp; Tenants
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            {topStats.map((s) => (
              <StatCard key={s.title} {...s} />
            ))}
          </div>
        </div>

        {/* Booking breakdown */}
        <div>
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
            Bookings
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            {bookingStats.map((s) => (
              <StatCard key={s.title} {...s} />
            ))}
          </div>
        </div>

        {/* Health / alerts */}
        <div>
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
            Platform Health
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {alertStats.map((s) => (
              <StatCard key={s.title} {...s} />
            ))}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
