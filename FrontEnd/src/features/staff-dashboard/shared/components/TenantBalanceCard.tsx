import { Wallet, RefreshCw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useTenantBalance } from '../hooks/useTenantPayments';

export default function TenantBalanceCard() {
  const { data, isLoading, isError } = useTenantBalance();

  return (
    <Card className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-none shadow-raised">
      <CardHeader className="pb-2">
        <CardTitle className="text-primary-foreground/80 text-sm font-medium flex items-center gap-2">
          <Wallet className="h-4 w-4" />
          Tenant Wallet Balance
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex items-center gap-2 py-2">
            <RefreshCw className="h-5 w-5 animate-spin opacity-70" />
            <span className="text-sm opacity-70">Loading...</span>
          </div>
        ) : isError || !data ? (
          <p className="text-sm opacity-70">Could not load balance.</p>
        ) : (
          <>
            <div className="text-4xl font-bold mb-1">
              {new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: data.currency,
              }).format(data.balance)}
            </div>
            <p className="text-xs text-primary-foreground/60">
              Last updated: {new Date(data.updatedAt).toLocaleString()}
            </p>
          </>
        )}
      </CardContent>
    </Card>
  );
}
