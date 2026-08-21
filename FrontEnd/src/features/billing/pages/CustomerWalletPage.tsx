import { useState } from 'react';
import { format } from 'date-fns';
import { CreditCard, ArrowDownLeft, ArrowUpRight, Plus, RefreshCw, Wallet } from 'lucide-react';
import { toast } from 'sonner';

import { useAppSelector } from '../../../redux/hooks';
import { selectUserId } from '../../../redux/selectors/authSelectors';
import { useWallet } from '../hooks/useWallet';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

export default function CustomerWalletPage() {
  const userId = useAppSelector(selectUserId);
  const { balanceQuery, getHistoryQuery, topUpMutation } = useWallet(userId ?? undefined);
  
  const [page, setPage] = useState(0);
  const size = 10;
  const historyQuery = getHistoryQuery(page, size);

  const [topUpAmount, setTopUpAmount] = useState('');
  const [isTopUpOpen, setIsTopUpOpen] = useState(false);

  const handleTopUp = async () => {
    const amount = parseFloat(topUpAmount);
    if (isNaN(amount) || amount <= 0) {
      toast.error('Please enter a valid positive amount.');
      return;
    }

    try {
      await topUpMutation.mutateAsync({
        customerId: userId!,
        amount,
      });
      toast.success(`Successfully added $${amount.toFixed(2)} to your wallet!`);
      setIsTopUpOpen(false);
      setTopUpAmount('');
    } catch (error) {
      toast.error('Failed to top up wallet. Please try again.');
    }
  };

  const isLoading = balanceQuery.isLoading || historyQuery.isLoading;

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center py-20">
        <div className="flex flex-col items-center gap-2 text-muted-foreground">
          <RefreshCw className="h-8 w-8 animate-spin" />
          <p>Loading wallet...</p>
        </div>
      </div>
    );
  }

  const balance = balanceQuery.data?.balance || 0;
  const currency = balanceQuery.data?.currency || 'USD';
  const transactions = historyQuery.data?.content || [];
  const totalPages = historyQuery.data?.totalPages || 0;

  return (
    <div className="space-y-6 max-w-5xl mx-auto py-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">My Wallet</h1>
          <p className="text-muted-foreground">Manage your balance and view transaction history.</p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {/* Balance Card */}
        <Card className="col-span-full md:col-span-1 lg:col-span-1 bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-none shadow-raised">
          <CardHeader>
            <CardTitle className="text-primary-foreground/80 text-sm font-medium flex items-center gap-2">
              <Wallet className="h-4 w-4" />
              Available Balance
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-4xl font-bold mb-4">
              {new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(balance)}
            </div>
            
            <Dialog open={isTopUpOpen} onOpenChange={setIsTopUpOpen}>
              <DialogTrigger asChild>
                <Button variant="secondary" className="w-full font-semibold shadow-low text-primary">
                  <Plus className="mr-2 h-4 w-4" />
                  Add Funds
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                  <DialogTitle>Top up your wallet</DialogTitle>
                  <DialogDescription>
                    Add funds to your wallet to instantly pay for bookings.
                  </DialogDescription>
                </DialogHeader>
                <div className="grid gap-4 py-4">
                  <div className="grid gap-2">
                    <Label htmlFor="amount">Amount (USD)</Label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">$</span>
                      <Input
                        id="amount"
                        type="number"
                        placeholder="50.00"
                        className="pl-8"
                        value={topUpAmount}
                        onChange={(e) => setTopUpAmount(e.target.value)}
                        min="1"
                        step="0.01"
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    {[10, 50, 100, 200].map((amt) => (
                      <Button
                        key={amt}
                        type="button"
                        variant="outline"
                        className="flex-1"
                        onClick={() => setTopUpAmount(amt.toString())}
                      >
                        +${amt}
                      </Button>
                    ))}
                  </div>
                </div>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setIsTopUpOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="button" onClick={handleTopUp} disabled={topUpMutation.isPending || !topUpAmount}>
                    {topUpMutation.isPending && <RefreshCw className="mr-2 h-4 w-4 animate-spin" />}
                    Confirm Top Up
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </CardContent>
        </Card>

        {/* Quick Stats or Info (Optional, taking up remaining grid space) */}
        <Card className="col-span-full md:col-span-1 lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-muted-foreground" />
              Wallet Information
            </CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground space-y-4">
            <p>
              Your wallet balance can be used to instantly pay for bookings across any of our branches. 
              Funds added to your wallet are non-refundable but never expire.
            </p>
            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 bg-muted/50 rounded-lg border border-border">
                <div className="font-semibold text-foreground mb-1">Instant Checkout</div>
                Skip the credit card forms when booking your next space.
              </div>
              <div className="p-4 bg-muted/50 rounded-lg border border-border">
                <div className="font-semibold text-foreground mb-1">Easy Refunds</div>
                Cancelled bookings are instantly refunded back to your wallet.
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Transaction History */}
      <Card>
        <CardHeader>
          <CardTitle>Transaction History</CardTitle>
          <CardDescription>A complete log of your deposits and payments.</CardDescription>
        </CardHeader>
        <CardContent>
          {transactions.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              <div className="mx-auto bg-muted h-12 w-12 rounded-full flex items-center justify-center mb-4">
                <CreditCard className="h-6 w-6" />
              </div>
              <p>No transactions yet.</p>
              <p className="text-sm">Top up your wallet to get started.</p>
            </div>
          ) : (
            <div className="rounded-md border border-border overflow-hidden">
              <Table>
                <TableHeader className="bg-muted/50">
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {transactions.map((txn) => {
                    const isDeposit = txn.transactionType === 'DEPOSIT' || txn.transactionType === 'REFUND';
                    return (
                      <TableRow key={txn.transactionId}>
                        <TableCell className="whitespace-nowrap">
                          <div className="font-medium text-foreground">
                            {format(new Date(txn.createdAt), 'MMM d, yyyy')}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {format(new Date(txn.createdAt), 'h:mm a')}
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge 
                            variant="secondary" 
                            className={
                              txn.transactionType === 'DEPOSIT' ? 'bg-green-100 text-green-700 hover:bg-green-100' :
                              txn.transactionType === 'REFUND' ? 'bg-blue-100 text-blue-700 hover:bg-blue-100' :
                              'bg-orange-100 text-orange-700 hover:bg-orange-100'
                            }
                          >
                            {txn.transactionType}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <span className="text-foreground">{txn.description}</span>
                          {txn.bookingId && (
                            <div className="text-xs text-muted-foreground mt-0.5 font-mono">
                              Booking Ref: {txn.bookingId.substring(0, 8)}...
                            </div>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className={`font-semibold flex items-center justify-end gap-1 ${isDeposit ? 'text-green-600' : 'text-foreground'}`}>
                            {isDeposit ? <ArrowDownLeft className="h-4 w-4" /> : <ArrowUpRight className="h-4 w-4 text-muted-foreground" />}
                            {isDeposit ? '+' : '-'}${txn.amount.toFixed(2)}
                          </div>
                          <div className="text-xs text-muted-foreground mt-0.5">
                            Bal: ${txn.balanceAfter.toFixed(2)}
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
          
          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between space-x-2 py-4">
              <div className="text-sm text-muted-foreground">
                Page {page + 1} of {totalPages}
              </div>
              <div className="space-x-2">
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
  );
}
