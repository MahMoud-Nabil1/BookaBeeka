import { useState } from 'react';
import {
  Building2,
  Users,
  ShieldAlert,
  ShieldCheck,
  Trash2,
  Loader2,
  Search,
  AlertTriangle,
  Hotel,
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
  useTenants,
  useSuspendTenant,
  useUnsuspendTenant,
  useDeleteTenant,
} from '../hooks/useSuperAdmin';
import type { TenantSummary } from '../../../../types/superAdmin';

export default function SuperAdminHotelsPage() {
  const { data: tenants = [], isLoading, isError } = useTenants(0, 100);
  const [search, setSearch] = useState('');

  // Modals state
  const [suspendTarget, setSuspendTarget] = useState<TenantSummary | null>(null);
  const [suspendReason, setSuspendReason] = useState('');

  const [unsuspendTarget, setUnsuspendTarget] = useState<TenantSummary | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<TenantSummary | null>(null);

  const { mutate: suspendTenant, isPending: isSuspending } = useSuspendTenant();
  const { mutate: unsuspendTenant, isPending: isUnsuspending } = useUnsuspendTenant();
  const { mutate: deleteTenant, isPending: isDeleting } = useDeleteTenant();

  const safeTenants = Array.isArray(tenants) ? tenants : [];
  const filteredTenants = safeTenants.filter((t) => {
    if (!t) return false;
    const name = (t.name || '').toLowerCase();
    const subdomain = (t.subdomain || '').toLowerCase();
    const query = search.toLowerCase();
    return name.includes(query) || subdomain.includes(query);
  });

  const handleConfirmSuspend = () => {
    if (!suspendTarget) return;
    suspendTenant(
      { tenantId: suspendTarget.id, reason: suspendReason },
      {
        onSuccess: () => {
          setSuspendTarget(null);
          setSuspendReason('');
        },
      }
    );
  };

  const handleConfirmUnsuspend = () => {
    if (!unsuspendTarget) return;
    unsuspendTenant(unsuspendTarget.id, {
      onSuccess: () => setUnsuspendTarget(null),
    });
  };

  const handleConfirmDelete = () => {
    if (!deleteTarget) return;
    deleteTenant(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  return (
    <PageLayout
      title="Hotels Management"
      description="View statistics, suspend operations, or permanently delete hotels across the platform."
    >
      <div className="space-y-6">
        {/* Search Bar & Summary Stats */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by hotel or subdomain..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span>Total Hotels:</span>
            <span className="font-semibold text-foreground">{tenants.length}</span>
          </div>
        </div>

        {/* Content Table */}
        <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center py-24 text-muted-foreground gap-3">
              <Loader2 className="h-6 w-6 animate-spin text-primary" />
              <span>Loading hotels...</span>
            </div>
          ) : isError ? (
            <div className="flex flex-col items-center justify-center py-20 text-destructive gap-2">
              <AlertTriangle className="h-8 w-8" />
              <p>Failed to load hotels. Please refresh the page.</p>
            </div>
          ) : filteredTenants.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
              <Hotel className="h-10 w-10 mb-3 opacity-40" />
              <p className="font-medium">No hotels found</p>
              {search && <p className="text-xs mt-1">Try refining your search keyword.</p>}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Hotel Property</TableHead>
                  <TableHead>Subdomain</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-center">Owners</TableHead>
                  <TableHead className="text-center">Admins</TableHead>
                  <TableHead>Currency / TZ</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTenants.map((hotel) => {
                  const isSuspended = hotel.status === 'SUSPENDED';
                  const isActive = hotel.status === 'ACTIVE';

                  return (
                    <TableRow key={hotel.id}>
                      <TableCell className="font-medium">
                        <div className="flex items-center gap-2.5">
                          <div className="p-2 rounded-lg bg-primary/10 text-primary">
                            <Building2 className="h-4 w-4" />
                          </div>
                          <div>
                            <span className="font-semibold text-foreground block">
                              {hotel.name}
                            </span>
                            <span className="text-xs text-muted-foreground font-mono">
                              {hotel.id.substring(0, 8)}...
                            </span>
                          </div>
                        </div>
                      </TableCell>

                      <TableCell>
                        <span className="text-xs font-mono bg-muted px-2 py-1 rounded">
                          {hotel.subdomain}
                        </span>
                      </TableCell>

                      <TableCell>
                        <Badge
                          variant={
                            isActive
                              ? 'default'
                              : isSuspended
                              ? 'secondary'
                              : 'destructive'
                          }
                          className={
                            isActive
                              ? 'bg-emerald-600 hover:bg-emerald-600'
                              : isSuspended
                              ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30'
                              : ''
                          }
                        >
                          {hotel.status}
                        </Badge>
                        {isSuspended && hotel.suspendedReason && (
                          <span
                            className="block text-xs text-muted-foreground mt-0.5 truncate max-w-[150px]"
                            title={hotel.suspendedReason}
                          >
                            Reason: {hotel.suspendedReason}
                          </span>
                        )}
                      </TableCell>

                      <TableCell className="text-center">
                        <div className="inline-flex items-center gap-1 text-sm font-medium">
                          <Users className="h-3.5 w-3.5 text-muted-foreground" />
                          <span>{hotel.ownersCount ?? 0}</span>
                        </div>
                      </TableCell>

                      <TableCell className="text-center">
                        <div className="inline-flex items-center gap-1 text-sm font-medium">
                          <Users className="h-3.5 w-3.5 text-muted-foreground" />
                          <span>{hotel.adminsCount ?? 0}</span>
                        </div>
                      </TableCell>

                      <TableCell className="text-xs text-muted-foreground">
                        <div>{hotel.currency}</div>
                        <div className="truncate max-w-[120px]">{hotel.timezone}</div>
                      </TableCell>

                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          {isActive ? (
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-amber-600 border-amber-300 hover:bg-amber-50 dark:hover:bg-amber-950/30 h-8 gap-1.5"
                              onClick={() => setSuspendTarget(hotel)}
                            >
                              <ShieldAlert className="h-3.5 w-3.5" />
                              Suspend
                            </Button>
                          ) : (
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-emerald-600 border-emerald-300 hover:bg-emerald-50 dark:hover:bg-emerald-950/30 h-8 gap-1.5"
                              onClick={() => setUnsuspendTarget(hotel)}
                            >
                              <ShieldCheck className="h-3.5 w-3.5" />
                              Unsuspend
                            </Button>
                          )}

                          <Button
                            variant="destructive"
                            size="sm"
                            className="h-8 gap-1.5"
                            onClick={() => setDeleteTarget(hotel)}
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

      {/* ── Suspend Confirmation Modal ────────────────────────────────────── */}
      <Dialog open={!!suspendTarget} onOpenChange={(open) => !open && setSuspendTarget(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-amber-600">
              <ShieldAlert className="h-5 w-5" />
              Suspend Hotel: {suspendTarget?.name}
            </DialogTitle>
            <DialogDescription>
              Suspending <strong>{suspendTarget?.name}</strong> will immediately hide all its
              rooms from customer discovery, catalog search, and prevent any new bookings.
              Existing confirmed bookings will remain safe and active.
            </DialogDescription>
          </DialogHeader>

          <div className="py-2 space-y-2">
            <label className="text-xs font-semibold text-muted-foreground uppercase">
              Reason for Suspension (optional)
            </label>
            <Input
              placeholder="e.g. Terms violation, maintenance, non-payment..."
              value={suspendReason}
              onChange={(e) => setSuspendReason(e.target.value)}
            />
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setSuspendTarget(null)}
              disabled={isSuspending}
            >
              Cancel
            </Button>
            <Button
              variant="default"
              className="bg-amber-600 hover:bg-amber-700 text-white"
              onClick={handleConfirmSuspend}
              disabled={isSuspending}
            >
              {isSuspending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Suspending...
                </>
              ) : (
                'Confirm Suspension'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Unsuspend Confirmation Modal ──────────────────────────────────── */}
      <Dialog
        open={!!unsuspendTarget}
        onOpenChange={(open) => !open && setUnsuspendTarget(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-600">
              <ShieldCheck className="h-5 w-5" />
              Unsuspend Hotel: {unsuspendTarget?.name}
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to restore active status to{' '}
              <strong>{unsuspendTarget?.name}</strong>? Its rooms will immediately become
              visible and bookable again on customer pages.
            </DialogDescription>
          </DialogHeader>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setUnsuspendTarget(null)}
              disabled={isUnsuspending}
            >
              Cancel
            </Button>
            <Button
              variant="default"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={handleConfirmUnsuspend}
              disabled={isUnsuspending}
            >
              {isUnsuspending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Restoring...
                </>
              ) : (
                'Confirm Unsuspend'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Destructive Delete Modal ───────────────────────────────────────── */}
      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <Trash2 className="h-5 w-5" />
              Delete Hotel: {deleteTarget?.name}
            </DialogTitle>
            <DialogDescription className="space-y-2">
              <span>
                Are you sure you want to permanently delete hotel{' '}
                <strong className="text-foreground">{deleteTarget?.name}</strong> (subdomain:{' '}
                <code>{deleteTarget?.subdomain}</code>)?
              </span>
              <span className="block text-destructive font-medium pt-2">
                Warning: This will permanently remove all rooms, room types, photos, and
                availability rules for this hotel.
              </span>
              <span className="block text-xs text-muted-foreground">
                Safety protection: Deletion will be blocked automatically if the hotel has any
                active or upcoming customer bookings.
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
                `Permanently Delete ${deleteTarget?.name || 'Hotel'}`
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
