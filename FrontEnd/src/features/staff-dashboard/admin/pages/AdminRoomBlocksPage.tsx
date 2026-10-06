import { useState } from 'react';
import { format } from 'date-fns';
import { Loader2, CalendarX2, Plus, Pencil, Trash2 } from 'lucide-react';
import PageLayout from '../../../../components/layout/PageLayout';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { useRoomBlocks, useDeleteRoomBlock } from '../../../catalog/hooks/useRoomBlocks';
import { useRooms } from '../../../catalog/hooks/useInventory';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';
import CreateRoomBlockModal from '../components/CreateRoomBlockModal';
import EditRoomBlockModal from '../components/EditRoomBlockModal';
import type { RoomBlockResponse } from '../../../../types/availability';

export default function AdminRoomBlocksPage() {
  const tenantId = useAppSelector(selectTenantId);
  const { data: roomBlocks = [], isLoading, isError } = useRoomBlocks();
  const { data: rooms = [] } = useRooms(tenantId || undefined);
  const { mutate: deleteRoomBlock } = useDeleteRoomBlock();

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedRoomBlock, setSelectedRoomBlock] = useState<RoomBlockResponse | null>(null);
  const [deletePopoverOpen, setDeletePopoverOpen] = useState<string | null>(null);

  // Helper to get room name from roomId
  const getRoomName = (roomId: string) => {
    const room = rooms.find((r) => r.id === roomId);
    return room?.name || `Room ${roomId.substring(0, 8)}...`;
  };

  const handleEdit = (roomBlock: RoomBlockResponse) => {
    setSelectedRoomBlock(roomBlock);
    setEditModalOpen(true);
  };

  const handleDelete = (id: string) => {
    deleteRoomBlock(id, {
      onSuccess: () => {
        setDeletePopoverOpen(null);
      },
    });
  };

  // Sort by start date (newest first)
  const sortedBlocks = [...roomBlocks].sort(
    (a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
  );

  return (
    <PageLayout title="Room Blocks">
      <div className="flex justify-between items-center mb-6">
        <p className="text-sm text-muted-foreground">
          Manage room availability blocks for maintenance, repairs, or other operational needs.
        </p>
        <Button onClick={() => setCreateModalOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Block a Room
        </Button>
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-20 text-muted-foreground gap-3">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span>Loading room blocks…</span>
        </div>
      )}

      {isError && (
        <p className="text-center text-destructive py-12">
          Failed to load room blocks. Please refresh.
        </p>
      )}

      {!isLoading && !isError && sortedBlocks.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-muted-foreground text-center">
          <CalendarX2 className="h-12 w-12 mb-4 opacity-30" />
          <p className="font-medium text-base mb-1">No room blocks found</p>
          <p className="text-sm">Block a room to prevent bookings during specific dates.</p>
        </div>
      )}

      {!isLoading && !isError && sortedBlocks.length > 0 && (
        <div className="border rounded-lg">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Room</TableHead>
                <TableHead>Start Date</TableHead>
                <TableHead>End Date</TableHead>
                <TableHead>Reason</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sortedBlocks.map((block) => (
                <TableRow key={block.id}>
                  <TableCell className="font-medium">
                    {getRoomName(block.roomId)}
                  </TableCell>
                  <TableCell>
                    {format(new Date(block.startDate), 'MMM d, yyyy')}
                  </TableCell>
                  <TableCell>
                    {format(new Date(block.endDate), 'MMM d, yyyy')}
                  </TableCell>
                  <TableCell className="max-w-xs">
                    {block.reason ? (
                      <span className="text-sm text-muted-foreground line-clamp-2">
                        {block.reason}
                      </span>
                    ) : (
                      <span className="text-sm text-muted-foreground italic">
                        No reason provided
                      </span>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleEdit(block)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      
                      <Popover
                        open={deletePopoverOpen === block.id}
                        onOpenChange={(open) =>
                          setDeletePopoverOpen(open ? block.id : null)
                        }
                      >
                        <PopoverTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </PopoverTrigger>
                        <PopoverContent className="w-80">
                          <div className="space-y-3">
                            <div>
                              <h4 className="font-semibold text-sm">Delete Room Block</h4>
                              <p className="text-sm text-muted-foreground mt-1">
                                Are you sure? This will allow bookings for{' '}
                                <span className="font-medium text-foreground">
                                  {getRoomName(block.roomId)}
                                </span>{' '}
                                during the blocked dates.
                              </p>
                            </div>
                            <div className="flex gap-2 justify-end">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setDeletePopoverOpen(null)}
                              >
                                Cancel
                              </Button>
                              <Button
                                variant="destructive"
                                size="sm"
                                onClick={() => handleDelete(block.id)}
                              >
                                Delete
                              </Button>
                            </div>
                          </div>
                        </PopoverContent>
                      </Popover>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Modals */}
      <CreateRoomBlockModal
        open={createModalOpen}
        onOpenChange={setCreateModalOpen}
      />
      <EditRoomBlockModal
        open={editModalOpen}
        onOpenChange={setEditModalOpen}
        roomBlock={selectedRoomBlock}
      />
    </PageLayout>
  );
}
