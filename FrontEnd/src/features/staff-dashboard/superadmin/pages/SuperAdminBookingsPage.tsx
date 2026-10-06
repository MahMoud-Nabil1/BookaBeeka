import { useState } from 'react';
import {
  Loader2, CalendarDays, Building2, CheckCircle2,
  XCircle, Clock, AlertTriangle, ChevronDown, ChevronUp,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '../../../../components/ui/badge';
import PageLayout from '../../../../components/layout/PageLayout';
import { useTenants, useTenantDetail } from '../hooks/useSuperAdmin';
import type { TenantSummary } from '../../../../types/superAdmin';

// ── Booking summary row per tenant ────────────────────────────────────────────
function TenantBookingRow({ tenant }: { tenant: TenantSummary }) {
  const [expanded, setExpanded] = useState(false);
  const { data: detail, isLoading } = useTenantDetail(expanded ? tenant.id : null);

  const stats = [
    {
      label: 'Total',
      value: detail?.totalBookings ?? '—',
      icon: CalendarDays,
      color: 'text-blue-600',
      bg: 'bg-blue-50 dark:bg-blue-950',
    },
    {
      label: 'Confirmed',
      value: detail?.confirmedBookings ?? '—',
      icon: CheckCircle2,
      color: 'text-green-600',
      bg: 'bg-green-50 dark:bg-green-950',
    },
    {
      label: 'Completed',
      value: detail?.completedBookings ?? '—',
      icon: CheckCircle2,
      color: 'text-primary',
      bg: 'bg-primary/10',
    },
    {
      label: 'Cancelled',
      value: detail?.cancelledBookings ?? '—',
      icon: XCircle,
      color: 'text-destructive',
      bg: 'bg-destructive/10',
    },
  ];

  return (
    <Card className="border-border">
      <button
        className="w-full text-left"
        onClick={() => setExpanded((v) => !v)}
        aria-expanded={expanded}
      >
        <CardHeader className="flex flex-row items-center justify-between py-4 px-5 cursor-pointer hover:bg-muted/40 transition-colors rounded-t-lg">
          <div className="flex items-center gap-3">
            <Building2 className="h-4 w-4 text-muted-foreground shrink-0" />
            <div>
              <CardTitle className="text-sm font-semibold">{tenant.name}</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">{tenant.subdomain}</p>
            </div>
            <Badge
              variant={
                tenant.status === 'ACTIVE'
                  ? 'default'
                  : tenant.status === 'SUSPENDED'
                  ? 'outline'
                  : 'destructive'
              }
              className="ml-2 text-xs"
            >
              {tenant.status}
            </Badge>
          </div>
          <div className="flex items-center gap-2">
            {expanded && isLoading && (
              <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
            )}
            {expanded ? (
              <ChevronUp className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            )}
          </div>
        </CardHeader>
      </button>

      {expanded && !isLoading && detail && (
        <CardContent className="pt-0 pb-5 px-5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
            {stats.map((s) => (
              <div
                key={s.label}
                className="flex items-center gap-3 rounded-lg border border-border p-3"
              >
                <div className={`p-1.5 rounded-md ${s.bg}`}>
                  <s.icon className={`h-3.5 w-3.5 ${s.color}`} />
                </div>
                <div>
                  <p className="text-lg font-bold text-foreground leading-none">{s.value}</p>
                  <p className="text-xs text-muted-foreground mt-0.5">{s.label}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Revenue snippet */}
          <div className="mt-4 rounded-lg bg-muted/50 px-4 py-3 flex items-center justify-between">
            <span className="text-sm text-muted-foreground">Tenant Revenue Balance</span>
            <span className="text-sm font-semibold text-foreground">
              {detail.currency}{' '}
              {new Intl.NumberFormat('en-US', { minimumFractionDigits: 2 }).format(
                detail.revenueBalance
              )}
            </span>
          </div>
        </CardContent>
      )}
    </Card>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function SuperAdminBookingsPage() {
  const { data: tenants = [], isLoading, isError } = useTenants();

  return (
    <PageLayout
      title="All Bookings"
      description="Booking stats per tenant. Click a tenant row to expand booking details."
    >
      {/* Info banner */}
      <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 dark:bg-amber-950/30 dark:border-amber-800 px-4 py-3 mb-6 text-sm text-amber-800 dark:text-amber-300">
        <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
        <span>
          Booking counts are sourced from tenant-level stats. Expand a tenant to see the breakdown.
        </span>
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-20 text-muted-foreground gap-3">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span>Loading tenants…</span>
        </div>
      )}

      {isError && (
        <p className="text-center text-destructive py-12">
          Failed to load tenant bookings. Please refresh.
        </p>
      )}

      {!isLoading && !isError && tenants.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground text-center">
          <CalendarDays className="h-12 w-12 mb-4 opacity-30" />
          <p className="font-medium">No tenants found</p>
        </div>
      )}

      {!isLoading && !isError && tenants.length > 0 && (
        <div className="space-y-3">
          {/* Platform-wide summary bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
            {[
              { label: 'Total Tenants', value: tenants.length, icon: Building2, color: 'text-primary', bg: 'bg-primary/10' },
              { label: 'Active', value: tenants.filter((t) => t.status === 'ACTIVE').length, icon: CheckCircle2, color: 'text-green-600', bg: 'bg-green-50 dark:bg-green-950' },
              { label: 'Suspended', value: tenants.filter((t) => t.status === 'SUSPENDED').length, icon: Clock, color: 'text-yellow-600', bg: 'bg-yellow-50 dark:bg-yellow-950' },
              { label: 'Banned', value: tenants.filter((t) => t.status === 'BANNED').length, icon: XCircle, color: 'text-destructive', bg: 'bg-destructive/10' },
            ].map((s) => (
              <div
                key={s.label}
                className="flex items-center gap-3 rounded-lg border border-border bg-background p-4"
              >
                <div className={`p-2 rounded-lg ${s.bg}`}>
                  <s.icon className={`h-4 w-4 ${s.color}`} />
                </div>
                <div>
                  <p className="text-xl font-bold text-foreground leading-none">{s.value}</p>
                  <p className="text-xs text-muted-foreground mt-0.5">{s.label}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Per-tenant rows */}
          {tenants.map((tenant) => (
            <TenantBookingRow key={tenant.id} tenant={tenant} />
          ))}
        </div>
      )}
    </PageLayout>
  );
}
