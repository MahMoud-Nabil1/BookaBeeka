import { useState } from 'react';
import { Tag, Loader2, Image, Plus, Pencil, Trash2 } from 'lucide-react';
import PageLayout from '../../../../components/layout/PageLayout';
import { Button } from '../../../../components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../../components/ui/table';
import { Badge } from '../../../../components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../../../../components/ui/dialog';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '../../../../components/ui/popover';
import { useRooms, useDeleteRoom } from '../../../catalog/hooks/useInventory';
import { useRoomAmenities } from '../../../catalog/hooks/useAmenities';
import { RoomAmenitiesManager } from '../components/RoomAmenitiesManager';
import { RoomPhotoManager } from '../components/RoomPhotoManager';
import { CreateRoomModal } from '../components/CreateRoomModal';
import { EditRoomModal } from '../components/EditRoomModal';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';
import type { RoomResponse } from '../../../../types/inventory';

export function AdminRoomsPage() {
  const tenantId = useAppSelector(selectTenantId);
  const { data: rooms = [], isLoading } = useRooms(tenantId ?? undefined);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<RoomResponse | null>(null);
  const [selectedRoom, setSelectedRoom] = useState<{ id: string; name: string } | null>(null);
  const [selectedRoomForPhotos, setSelectedRoomForPhotos] = useState<{ id: string; name: string } | null>(null);

  return (
    <PageLayout
      title="Rooms Management"
      description="Create and manage your hotel rooms"
    >
      <div className="space-y-6">
        {/* Header row with Add Room button */}
        <div className="flex justify-between items-center">
          <p className="text-sm text-muted-foreground">
            {rooms.length} {rooms.length === 1 ? 'room' : 'rooms'} in your inventory
          </p>
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Room
          </Button>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin mr-3 text-primary" />
            <span className="text-muted-foreground">Loading rooms...</span>
          </div>
        ) : rooms.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 border border-dashed rounded-lg">
            <p className="text-muted-foreground mb-4">No rooms yet</p>
            <Button onClick={() => setIsCreateOpen(true)} variant="outline">
              <Plus className="mr-2 h-4 w-4" />
              Create Your First Room
            </Button>
          </div>
        ) : (
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Room Name</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Capacity</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Amenities</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rooms.map((room) => (
                  <RoomRow
                    key={room.id}
                    room={room}
                    onEdit={() => setEditingRoom(room)}
                    onManageAmenities={() => setSelectedRoom({ id: room.id, name: room.name })}
                    onManagePhotos={() => setSelectedRoomForPhotos({ id: room.id, name: room.name })}
                  />
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {/* Create room modal */}
      <CreateRoomModal open={isCreateOpen} onOpenChange={setIsCreateOpen} />

      {/* Edit room modal */}
      {editingRoom && (
        <EditRoomModal
          room={editingRoom}
          open={!!editingRoom}
          onOpenChange={(open) => !open && setEditingRoom(null)}
        />
      )}

      {/* Amenities manager */}
      {selectedRoom && (
        <RoomAmenitiesManager
          roomId={selectedRoom.id}
          roomName={selectedRoom.name}
          open={!!selectedRoom}
          onOpenChange={(open) => !open && setSelectedRoom(null)}
        />
      )}

      {/* Photos manager */}
      {selectedRoomForPhotos && (
        <Dialog
          open={!!selectedRoomForPhotos}
          onOpenChange={(open) => !open && setSelectedRoomForPhotos(null)}
        >
          <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Manage Photos — {selectedRoomForPhotos.name}</DialogTitle>
            </DialogHeader>
            <RoomPhotoManager resourceId={selectedRoomForPhotos.id} />
          </DialogContent>
        </Dialog>
      )}
    </PageLayout>
  );
}

// ── Row component ─────────────────────────────────────────────────────────────

function RoomRow({
  room,
  onEdit,
  onManageAmenities,
  onManagePhotos,
}: {
  room: RoomResponse;
  onEdit: () => void;
  onManageAmenities: () => void;
  onManagePhotos: () => void;
}) {
  const { data: amenities = [], isLoading: loadingAmenities } = useRoomAmenities(room.id);
  const deleteRoom = useDeleteRoom();
  const [deleteOpen, setDeleteOpen] = useState(false);

  return (
    <TableRow>
      <TableCell className="font-medium">{room.name}</TableCell>
      <TableCell>
        <Badge variant="secondary">
          {room.roomCategory.charAt(0) + room.roomCategory.slice(1).toLowerCase()}
        </Badge>
      </TableCell>
      <TableCell>{room.capacity} guests</TableCell>
      <TableCell>
        <Badge variant={room.isActive ? 'default' : 'outline'}>
          {room.isActive ? 'Active' : 'Inactive'}
        </Badge>
      </TableCell>
      <TableCell>
        {loadingAmenities ? (
          <span className="text-xs text-muted-foreground">Loading...</span>
        ) : amenities.length === 0 ? (
          <span className="text-xs text-muted-foreground">None</span>
        ) : (
          <div className="flex flex-wrap gap-1">
            {amenities.slice(0, 3).map((amenity) => (
              <Badge key={amenity.id} variant="outline" className="text-xs">
                {amenity.icon && <span className="mr-1">{amenity.icon}</span>}
                {amenity.name}
              </Badge>
            ))}
            {amenities.length > 3 && (
              <Badge variant="outline" className="text-xs">
                +{amenities.length - 3} more
              </Badge>
            )}
          </div>
        )}
      </TableCell>
      <TableCell className="text-right">
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="sm" onClick={onManagePhotos}>
            <Image className="h-4 w-4 mr-1" />
            Photos
          </Button>
          <Button variant="ghost" size="sm" onClick={onManageAmenities}>
            <Tag className="h-4 w-4 mr-1" />
            Amenities
          </Button>
          <Button variant="ghost" size="sm" onClick={onEdit}>
            <Pencil className="h-4 w-4" />
          </Button>
          <Popover open={deleteOpen} onOpenChange={setDeleteOpen}>
            <PopoverTrigger asChild>
              <Button variant="ghost" size="sm">
                <Trash2 className="h-4 w-4 text-destructive" />
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-72">
              <div className="space-y-4">
                <div>
                  <h4 className="font-semibold">Delete Room</h4>
                  <p className="text-sm text-muted-foreground mt-1">
                    Are you sure you want to delete "{room.name}"? This cannot be undone.
                  </p>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" size="sm" onClick={() => setDeleteOpen(false)}>
                    Cancel
                  </Button>
                  <Button
                    variant="destructive"
                    size="sm"
                    disabled={deleteRoom.isPending}
                    onClick={() =>
                      deleteRoom.mutate(room.id, { onSuccess: () => setDeleteOpen(false) })
                    }
                  >
                    {deleteRoom.isPending ? 'Deleting…' : 'Delete'}
                  </Button>
                </div>
              </div>
            </PopoverContent>
          </Popover>
        </div>
      </TableCell>
    </TableRow>
  );
}
