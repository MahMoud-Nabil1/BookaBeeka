import { Loader2, BedDouble } from 'lucide-react';
import PageLayout from '../../../components/layout/PageLayout';
import RoomCard from '../components/RoomCard';
import { useRooms, useAllRoomTypes } from '../hooks/useInventory';
import { useAppSelector } from '../../../redux/hooks';
import { selectTenantId } from '../../../redux/selectors/authSelectors';
import type { RoomTypeResponse } from '../../../types/inventory';

export default function CatalogPage() {
  const tenantId = useAppSelector(selectTenantId);

  const { data: rooms, isLoading: roomsLoading, isError: roomsError } = useRooms(tenantId ?? undefined);
  const { data: roomTypes } = useAllRoomTypes(tenantId ?? undefined);

  // Build a map of roomId → first linked room type for price/duration display.
  // The per-room types endpoint exists but we'll use the flat room types list
  // to avoid N+1 requests on the catalog page.
  const roomTypeForRoom = (roomId: string): RoomTypeResponse | undefined => {
    // Room types don't have a direct roomId field in the flat list —
    // we'll show the cheapest active room type as a default price indicator.
    return roomTypes?.filter((rt) => rt.isActive).sort((a, b) => a.price - b.price)[0];
  };

  const availableRooms = rooms?.filter((r) => r.isActive && r.isBookable) ?? [];

  if (roomsLoading) {
    return (
      <PageLayout title="Browse Rooms">
        <div className="flex justify-center items-center py-24 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin mr-3" />
          <span>Loading available rooms...</span>
        </div>
      </PageLayout>
    );
  }

  if (roomsError) {
    return (
      <PageLayout title="Browse Rooms">
        <div className="flex flex-col items-center justify-center py-24 text-center text-muted-foreground">
          <BedDouble className="h-12 w-12 mb-4 opacity-40" />
          <p className="text-lg font-medium">Could not load rooms</p>
          <p className="text-sm mt-1">Please try refreshing the page.</p>
        </div>
      </PageLayout>
    );
  }

  if (availableRooms.length === 0) {
    return (
      <PageLayout title="Browse Rooms">
        <div className="flex flex-col items-center justify-center py-24 text-center text-muted-foreground">
          <BedDouble className="h-12 w-12 mb-4 opacity-40" />
          <p className="text-lg font-medium">No rooms available</p>
          <p className="text-sm mt-1">Check back soon — new rooms are being added.</p>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout title="Browse Rooms">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {availableRooms.map((room) => (
          <RoomCard
            key={room.id}
            room={room}
            roomType={roomTypeForRoom(room.id)}
          />
        ))}
      </div>
    </PageLayout>
  );
}
