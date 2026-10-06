import { useState } from 'react';
import {
  Loader2, DoorOpen, ChevronDown, ChevronUp,
  Building2, Users, CheckCircle2, XCircle, X,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '../../../../components/ui/badge';
import { Button } from '../../../../components/ui/button';
import PageLayout from '../../../../components/layout/PageLayout';
import { useTenants, useTenantRooms } from '../hooks/useSuperAdmin';
import type { TenantSummary } from '../../../../types/superAdmin';

// ── Room detail slide-over card ───────────────────────────────────────────────
interface RoomDetailCardProps {
  room: any;
  tenantName: string;
  onClose: () => void;
}

function RoomDetailCard({ room, tenantName, onClose }: RoomDetailCardProps) {
  return (
    <div className="fixed inset-0 z-50 flex justify-end" aria-modal="true">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Panel */}
      <div className="relative w-full max-w-md bg-background border-l border-border shadow-2xl flex flex-col h-full overflow-y-auto animate-in slide-in-from-right duration-300">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <div className="flex items-center gap-2">
            <DoorOpen className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-semibold text-foreground">{room.name}</h2>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Body */}
        <div className="flex-1 p-6 space-y-6">
          {/* Status badges */}
          <div className="flex gap-2 flex-wrap">
            <Badge variant={room.isActive ? 'default' : 'outline'}>
              {room.isActive ? (
                <><CheckCircle2 className="h-3 w-3 mr-1" /> Active</>
              ) : (
                <><XCircle className="h-3 w-3 mr-1" /> Inactive</>
              )}
            </Badge>
            <Badge variant={room.isBookable ? 'secondary' : 'outline'}>
              {room.isBookable ? 'Bookable' : 'Not Bookable'}
            </Badge>
          </div>

          {/* Info grid */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Tenant</p>
              <div className="flex items-center gap-1.5">
                <Building2 className="h-3.5 w-3.5 text-muted-foreground" />
                <p className="text-sm font-medium">{tenantName}</p>
              </div>
            </div>

            <div className="space-y-1">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Category</p>
              <Badge variant="secondary">{room.resourceType ?? room.roomCategory ?? '—'}</Badge>
            </div>

            <div className="space-y-1">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Capacity</p>
              <div className="flex items-center gap-1.5">
                <Users className="h-3.5 w-3.5 text-muted-foreground" />
                <p className="text-sm font-medium">{room.capacity} guests</p>
              </div>
            </div>

            <div className="space-y-1">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Room ID</p>
              <p className="text-xs text-muted-foreground font-mono break-all">{room.id}</p>
            </div>
          </div>

          {/* Specs */}
          {room.specs && Object.keys(room.specs).length > 0 && (
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Specs</p>
              <div className="rounded-lg bg-muted p-3 space-y-3">
                {Object.entries(room.specs).map(([key, val]) => {
                  const raw = String(val);
                  // If the value contains commas it's a list — render as tags
                  const isTagList = raw.includes(',');
                  const tags = isTagList ? raw.split(',').map((t) => t.trim()).filter(Boolean) : [];

                  return (
                    <div key={key} className={isTagList ? 'space-y-1.5' : 'flex justify-between items-center text-sm'}>
                      <span className="text-muted-foreground capitalize text-sm">
                        {key.replace(/_/g, ' ')}
                      </span>
                      {isTagList ? (
                        <div className="flex flex-wrap gap-1.5 mt-1">
                          {tags.map((tag) => (
                            <span
                              key={tag}
                              className="inline-flex items-center rounded-full bg-background border border-border px-2.5 py-0.5 text-xs font-medium text-foreground"
                            >
                              {tag}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="font-medium text-sm text-right">{raw}</span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Created at */}
          <div className="space-y-1">
            <p className="text-xs text-muted-foreground uppercase tracking-wider">Created</p>
            <p className="text-sm">
              {room.createdAt
                ? new Date(room.createdAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })
                : '—'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Tenant room group ─────────────────────────────────────────────────────────
function TenantRoomGroup({
  tenant,
  onSelectRoom,
}: {
  tenant: TenantSummary;
  onSelectRoom: (room: any, tenantName: string) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const { data: rooms = [], isLoading } = useTenantRooms(expanded ? tenant.id : null);

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
          {expanded ? (
            <ChevronUp className="h-4 w-4 text-muted-foreground" />
          ) : (
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          )}
        </CardHeader>
      </button>

      {expanded && (
        <CardContent className="pt-0 pb-4 px-5">
          {isLoading ? (
            <div className="flex items-center gap-2 py-4 text-muted-foreground text-sm">
              <Loader2 className="h-4 w-4 animate-spin" />
              Loading rooms…
            </div>
          ) : rooms.length === 0 ? (
            <p className="text-sm text-muted-foreground py-4 text-center">
              No rooms found for this tenant.
            </p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 pt-2">
              {rooms.map((room: any) => (
                <button
                  key={room.id}
                  onClick={() => onSelectRoom(room, tenant.name)}
                  className="group text-left rounded-lg border border-border p-3 hover:border-primary hover:shadow-sm transition-all bg-background"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-foreground truncate group-hover:text-primary transition-colors">
                        {room.name}
                      </p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        {room.resourceType ?? room.roomCategory} · {room.capacity} guests
                      </p>
                    </div>
                    <Badge
                      variant={room.isActive ? 'default' : 'outline'}
                      className="text-xs shrink-0"
                    >
                      {room.isActive ? 'Active' : 'Off'}
                    </Badge>
                  </div>
                </button>
              ))}
            </div>
          )}
        </CardContent>
      )}
    </Card>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function SuperAdminRoomsPage() {
  const { data: tenants = [], isLoading, isError } = useTenants();
  const [selectedRoom, setSelectedRoom] = useState<{ room: any; tenantName: string } | null>(null);

  return (
    <PageLayout
      title="All Rooms"
      description="Every room across all tenants on the platform. Click a tenant to expand, then click a room for details."
    >
      {isLoading && (
        <div className="flex items-center justify-center py-20 text-muted-foreground gap-3">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span>Loading tenants…</span>
        </div>
      )}

      {isError && (
        <p className="text-center text-destructive py-12">
          Failed to load tenants. Please refresh.
        </p>
      )}

      {!isLoading && !isError && tenants.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground text-center">
          <DoorOpen className="h-12 w-12 mb-4 opacity-30" />
          <p className="font-medium">No tenants found</p>
        </div>
      )}

      {!isLoading && !isError && tenants.length > 0 && (
        <div className="space-y-3">
          {tenants.map((tenant) => (
            <TenantRoomGroup
              key={tenant.id}
              tenant={tenant}
              onSelectRoom={(room, name) => setSelectedRoom({ room, tenantName: name })}
            />
          ))}
        </div>
      )}

      {selectedRoom && (
        <RoomDetailCard
          room={selectedRoom.room}
          tenantName={selectedRoom.tenantName}
          onClose={() => setSelectedRoom(null)}
        />
      )}
    </PageLayout>
  );
}
