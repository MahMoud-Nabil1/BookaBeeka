import { useState } from 'react';
import {
  Loader2, CreditCard, TrendingUp, ArrowUpRight,
  ArrowDownRight, RefreshCw, DollarSign,
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '../../../../components/ui/button';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '../../../../components/ui/table';
import PageLayout from '../../../../components/layout/PageLayout';
import { usePlatformTransactions, usePlatformStats } from '../hooks/useSuperAdmin';
import type { PlatformTransaction } from '../../../../types/superAdmin';

// ── Transaction type badge ────────────────────────────────────────────────────
function TxTypeBadge({ type }: { type: string }) {
  const config: Record<string, { label: string; className: string; icon: React.ElementType }> = {
    PAYMENT:  { label: 'Payment',  className: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',   icon: ArrowUpRight },
    DEPOSIT:  { label: 'Deposit',  className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200', icon: ArrowDownRight },
    REFUND:   { label: 'Refund',   className: 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200', icon: RefreshCw },
  };
  const c = config[type] ?? { label: type, className: 'bg-muted text-muted-foreground', icon: CreditCard };
  const Icon = c.icon;
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${c.className}`}>
      <Icon className="h-3 w-3" />
      {c.label}
    </span>
  );
}

// ── Transaction row ───────────────────────────────────────────────────────────
function TransactionRow({ tx }: { tx: PlatformTransaction }) {
  const date = new Date(tx.createdAt);
  return (
    <TableRow className="hover:bg-muted/40">
      <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
        {date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
        <span className="block text-xs opacity-70">
          {date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}
        </span>
      </TableCell>
      <TableCell>
        <TxTypeBadge type={tx.transactionType} />
      </TableCell>
      <TableCell className="text-sm">
        <p className="font-medium text-foreground">{tx.fromCustomerName}</p>
        <p className="text-xs text-muted-foreground">{tx.fromCustomerEmail}</p>
      </TableCell>
      <TableCell>
        {tx.bookingId ? (
          <span className="font-mono text-xs text-muted-foreground">
            #{tx.bookingId.substring(0, 8)}
          </span>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        )}
      </TableCell>
      <TableCell className="text-right">
        <span
          className={`text-sm font-semibold ${
            tx.transactionType === 'REFUND'
              ? 'text-orange-600'
              : tx.transactionType === 'DEPOSIT'
              ? 'text-green-600'
              : 'text-foreground'
          }`}
        >
          {tx.transactionType === 'REFUND' ? '-' : '+'}
          {new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 2,
          }).format(tx.amount)}
        </span>
      </TableCell>
    </TableRow>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function SuperAdminPaymentsPage() {
  const [page, setPage] = useState(0);
  const size = 20;

  const { data: stats } = usePlatformStats();
  const { data, isLoading, isError } = usePlatformTransactions(page, size);

  const transactions = data?.content ?? [];
  const totalPages   = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const fmt = (n: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

  return (
    <PageLayout
      title="Platform Payments"
      description="All money movement across the platform — deposits, payments, and refunds."
    >
      <div className="space-y-6">
        {/* Summary cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Platform Revenue
              </CardTitle>
              <div className="p-2 rounded-lg bg-green-50 dark:bg-green-950">
                <TrendingUp className="h-4 w-4 text-green-600" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {stats ? fmt(stats.platformRevenue) : '—'}
              </p>
              <p className="text-xs text-muted-foreground mt-1">Tenant wallet balances total</p>
            </CardContent>
          </Card>

          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Money in Circulation
              </CardTitle>
              <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-950">
                <DollarSign className="h-4 w-4 text-blue-600" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {stats ? fmt(stats.moneyInCirculation) : '—'}
              </p>
              <p className="text-xs text-muted-foreground mt-1">All wallets combined</p>
            </CardContent>
          </Card>

          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Total Transactions
              </CardTitle>
              <div className="p-2 rounded-lg bg-primary/10">
                <CreditCard className="h-4 w-4 text-primary" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{totalElements || '—'}</p>
              <p className="text-xs text-muted-foreground mt-1">All time</p>
            </CardContent>
          </Card>
        </div>

        {/* Transaction table */}
        <Card className="border-border">
          <CardHeader>
            <CardTitle>Transaction History</CardTitle>
            <CardDescription>All platform-wide money movement, newest first.</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex items-center justify-center py-10 text-muted-foreground gap-2">
                <Loader2 className="h-5 w-5 animate-spin" />
                <span className="text-sm">Loading transactions…</span>
              </div>
            ) : isError ? (
              <p className="text-center text-destructive py-8 text-sm">
                Failed to load transactions. Please refresh.
              </p>
            ) : transactions.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">
                No transactions found.
              </p>
            ) : (
              <div className="rounded-md border border-border overflow-hidden">
                <Table>
                  <TableHeader className="bg-muted/50">
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Customer</TableHead>
                      <TableHead>Booking</TableHead>
                      <TableHead className="text-right">Amount</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {transactions.map((tx) => (
                      <TransactionRow key={tx.transactionId} tx={tx} />
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}

            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4">
                <span className="text-sm text-muted-foreground">
                  Page {page + 1} of {totalPages}
                </span>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </PageLayout>
  );
}
