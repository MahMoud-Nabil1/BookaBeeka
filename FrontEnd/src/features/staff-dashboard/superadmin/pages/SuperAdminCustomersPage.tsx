import { useState } from 'react';
import {
  Users,
  UserX,
  UserCheck,
  Trash2,
  Loader2,
  Search,
  AlertTriangle,
  Wallet,
} from 'lucide-react';
import PageLayout from '../../../../components/layout/PageLayout';
import { Button } from '../../../../components/ui/button';
import { Input } from '../../../../components/ui/input';
import { Badge } from '../../../../components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../../../components/ui/dialog';
import {
  useCustomers,
  useBanCustomer,
  useUnbanCustomer,
  useDeleteCustomer,
} from '../hooks/useSuperAdmin';
import type { CustomerSummary } from '../../../../types/superAdmin';

export default function SuperAdminCustomersPage() {
  const { data: pageData, isLoading, isError } = useCustomers(0, 100);
  const customers = pageData?.content ?? [];
  const [search, setSearch] = useState('');

  // Modals state
  const [banTarget, setBanTarget] = useState<CustomerSummary | null>(null);
  const [banReason, setBanReason] = useState('');

  const [unbanTarget, setUnbanTarget] = useState<CustomerSummary | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<CustomerSummary | null>(null);

  const { mutate: banCustomer, isPending: isBanning } = useBanCustomer();
  const { mutate: unbanCustomer, isPending: isUnbanning } = useUnbanCustomer();
  const { mutate: deleteCustomer, isPending: isDeleting } = useDeleteCustomer();

  const safeCustomers = Array.isArray(customers) ? customers : [];
  const filteredCustomers = safeCustomers.filter((c) => {
    if (!c) return false;
    const email = (c.email || '').toLowerCase();
    const firstName = (c.firstName || '').toLowerCase();
    const lastName = (c.lastName || '').toLowerCase();
    const phone = c.phone || '';
    const query = (search || '').toLowerCase();
    return email.includes(query) || firstName.includes(query) || lastName.includes(query) || phone.includes(query);
  });

  const handleConfirmBan = () => {
    if (!banTarget) return;
    banCustomer(
      { customerId: banTarget.id, reason: banReason },
      {
        onSuccess: () => {
          setBanTarget(null);
          setBanReason('');
        },
      }
    );
  };

  const handleConfirmUnban = () => {
    if (!unbanTarget) return;
    unbanCustomer(unbanTarget.id, {
      onSuccess: () => setUnbanTarget(null),
    });
  };

  const handleConfirmDelete = () => {
    if (!deleteTarget) return;
    deleteCustomer(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  return (
    <PageLayout
      title="Customer Management"
      description="Manage registered guest accounts, issue or lift booking bans, and handle account deletions."
    >
      <div className="space-y-6">
        {/* Search Bar & Summary Stats */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by name, email, or phone..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span>Total Customers:</span>
            <span className="font-semibold text-foreground">
              {pageData?.totalElements ?? customers.length}
            </span>
          </div>
        </div>

        {/* Content Table */}
        <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center py-24 text-muted-foreground gap-3">
              <Loader2 className="h-6 w-6 animate-spin text-primary" />
              <span>Loading customer accounts...</span>
            </div>
          ) : isError ? (
            <div className="flex flex-col items-center justify-center py-20 text-destructive gap-2">
              <AlertTriangle className="h-8 w-8" />
              <p>Failed to load customers. Please refresh the page.</p>
            </div>
          ) : filteredCustomers.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
              <Users className="h-10 w-10 mb-3 opacity-40" />
              <p className="font-medium">No customers found</p>
              {search && <p className="text-xs mt-1">Try refining your search keyword.</p>}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Customer</TableHead>
                  <TableHead>Phone</TableHead>
                  <TableHead>Booking Status</TableHead>
                  <TableHead>Wallet Balance</TableHead>
                  <TableHead>Joined</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCustomers.map((cust) => {
                  const isBanned = Boolean(cust.banned);

                  return (
                    <TableRow key={cust.id}>
                      <TableCell className="font-medium">
                        <div>
                          <span className="font-semibold text-foreground block">
                            {cust.firstName} {cust.lastName}
                          </span>
                          <span className="text-xs text-muted-foreground">
                            {cust.email}
                          </span>
                        </div>
                      </TableCell>

                      <TableCell className="text-xs text-muted-foreground">
                        {cust.phone || '—'}
                      </TableCell>

                      <TableCell>
                        <Badge
                          variant={isBanned ? 'destructive' : 'default'}
                          className={
                            !isBanned
                              ? 'bg-emerald-600 hover:bg-emerald-600'
                              : 'bg-rose-600 hover:bg-rose-600'
                          }
                        >
                          {isBanned ? 'Banned' : 'Active'}
                        </Badge>
                        {isBanned && cust.banReason && (
                          <span
                            className="block text-xs text-muted-foreground mt-0.5 truncate max-w-[180px]"
                            title={cust.banReason}
                          >
                            Reason: {cust.banReason}
                          </span>
                        )}
                      </TableCell>

                      <TableCell>
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <Wallet className="h-3.5 w-3.5 text-primary" />
                          <span>
                            {cust.walletBalance != null
                              ? `$${Number(cust.walletBalance).toFixed(2)}`
                              : '$0.00'}
                          </span>
                        </div>
                      </TableCell>

                      <TableCell className="text-xs text-muted-foreground">
                        {cust.createdAt
                          ? new Date(cust.createdAt).toLocaleDateString('en-US', {
                              year: 'numeric',
                              month: 'short',
                              day: 'numeric',
                            })
                          : '—'}
                      </TableCell>

                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          {isBanned ? (
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-emerald-600 border-emerald-300 hover:bg-emerald-50 dark:hover:bg-emerald-950/30 h-8 gap-1.5"
                              onClick={() => setUnbanTarget(cust)}
                            >
                              <UserCheck className="h-3.5 w-3.5" />
                              Unban
                            </Button>
                          ) : (
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-amber-600 border-amber-300 hover:bg-amber-50 dark:hover:bg-amber-950/30 h-8 gap-1.5"
                              onClick={() => setBanTarget(cust)}
                            >
                              <UserX className="h-3.5 w-3.5" />
                              Ban
                            </Button>
                          )}

                          <Button
                            variant="destructive"
                            size="sm"
                            className="h-8 gap-1.5"
                            onClick={() => setDeleteTarget(cust)}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                            Delete
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </div>
      </div>

      {/* ── Ban Customer Modal ─────────────────────────────────────────────── */}
      <Dialog open={!!banTarget} onOpenChange={(open) => !open && setBanTarget(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <UserX className="h-5 w-5" />
              Ban Customer: {[banTarget?.firstName, banTarget?.lastName].filter(Boolean).join(' ') || banTarget?.email || 'Customer'}
            </DialogTitle>
            <DialogDescription>
              Banning <strong>{banTarget?.email}</strong> will block them from making any new
              hotel room bookings immediately. They can still log in and view their account.
            </DialogDescription>
          </DialogHeader>

          <div className="py-2 space-y-2">
            <label className="text-xs font-semibold text-muted-foreground uppercase">
              Reason for Ban (optional)
            </label>
            <Input
              placeholder="e.g. Fraudulent activity, policy violations..."
              value={banReason}
              onChange={(e) => setBanReason(e.target.value)}
            />
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setBanTarget(null)}
              disabled={isBanning}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirmBan}
              disabled={isBanning}
            >
              {isBanning ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Banning...
                </>
              ) : (
                'Confirm Ban'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Unban Customer Modal ───────────────────────────────────────────── */}
      <Dialog open={!!unbanTarget} onOpenChange={(open) => !open && setUnbanTarget(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-600">
              <UserCheck className="h-5 w-5" />
              Unban Customer: {[unbanTarget?.firstName, unbanTarget?.lastName].filter(Boolean).join(' ') || unbanTarget?.email || 'Customer'}
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to lift the ban on{' '}
              <strong>{unbanTarget?.email}</strong>? They will be allowed to book rooms again.
            </DialogDescription>
          </DialogHeader>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setUnbanTarget(null)}
              disabled={isUnbanning}
            >
              Cancel
            </Button>
            <Button
              variant="default"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={handleConfirmUnban}
              disabled={isUnbanning}
            >
              {isUnbanning ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Unbanning...
                </>
              ) : (
                'Confirm Unban'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Destructive Delete Customer Modal ──────────────────────────────── */}
      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <Trash2 className="h-5 w-5" />
              Delete Customer: {[deleteTarget?.firstName, deleteTarget?.lastName].filter(Boolean).join(' ') || deleteTarget?.email || 'Customer'}
            </DialogTitle>
            <DialogDescription className="space-y-2">
              <span>
                Are you sure you want to permanently delete customer account{' '}
                <strong className="text-foreground">{deleteTarget?.email}</strong>?
              </span>
              <span className="block text-destructive font-medium pt-2">
                This will delete the customer profile, wallet, and personal notifications.
                Historical bookings and payments will be preserved for hotel accounting.
              </span>
              <span className="block text-xs text-muted-foreground">
                Safety protection: Deletion will be blocked if the customer has an active or
                upcoming room booking.
              </span>
            </DialogDescription>
          </DialogHeader>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setDeleteTarget(null)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirmDelete}
              disabled={isDeleting}
            >
              {isDeleting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Deleting...
                </>
              ) : (
                `Permanently Delete ${deleteTarget?.firstName || deleteTarget?.email || 'Customer'}`
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
