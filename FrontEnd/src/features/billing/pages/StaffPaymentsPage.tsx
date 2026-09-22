import { useState } from 'react';
import { RefreshCw } from 'lucide-react';
import TenantBalanceCard from '../../staff-dashboard/shared/components/TenantBalanceCard';
import TenantTransactionRow from '../../staff-dashboard/shared/components/TenantTransactionRow';
import { useTenantPaymentHistory } from '../../staff-dashboard/shared/hooks/useTenantPayments';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import PageLayout from '../../../components/layout/PageLayout';

interface StaffPaymentsPageProps {
  title?: string;
}

export default function StaffPaymentsPage({ title = 'Payments' }: StaffPaymentsPageProps) {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, isError } = useTenantPaymentHistory(page, size);

  const payments    = data?.content ?? [];
  const totalPages  = data?.totalPages ?? 0;

  return (
    <PageLayout title={title}>
      <div className="space-y-6">
        {/* Balance card */}
        <div className="max-w-sm">
          <TenantBalanceCard />
        </div>

        {/* Transaction history */}
        <Card className="border-border">
          <CardHeader>
            <CardTitle>Payment History</CardTitle>
            <CardDescription>All payments processed through this tenant.</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex items-center justify-center py-10 text-muted-foreground gap-2">
                <RefreshCw className="h-5 w-5 animate-spin" />
                <span className="text-sm">Loading payments…</span>
              </div>
            ) : isError ? (
              <p className="text-center text-destructive py-8 text-sm">
                Failed to load payment history. Please refresh.
              </p>
            ) : payments.length === 0 ? (
              <p className="text-center text-muted-foreground py-10 text-sm">
                No payment records found.
              </p>
            ) : (
              <div className="rounded-md border border-border overflow-hidden">
                <Table>
                  <TableHeader className="bg-muted/50">
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Booking</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Method</TableHead>
                      <TableHead className="text-right">Amount</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {payments.map((p) => (
                      <TenantTransactionRow key={p.paymentId} payment={p} />
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
                    variant="outline" size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline" size="sm"
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
