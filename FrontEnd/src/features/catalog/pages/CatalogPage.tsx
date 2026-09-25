import { Loader2, BedDouble } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import PageLayout from '../../../components/layout/PageLayout';
import RoomCard from '../components/RoomCard';
import api from '../../../config/api';
import type { RoomResponse, RoomTypeResponse } from '../../../types/inventory';

// Public API to get all available rooms across all tenants (for customer browsing)
async function getAllAvailableRooms(): Promise<RoomResponse[]> {
  // For now, we'll use the availability search endpoint
  // This is a temporary solution until a proper public rooms endpoint is created
  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
  
  try {
    const response = await api.get(`/api/availability/search`, {
      params: {
        checkIn: today,
        checkOut: tomorrow,
        page: 0,
        size: 100
      }
    });
    
    // Map availability response to room response format
    return response.data.content?.map((item: any) => ({
      id: item.room?.id || item.roomInfo?.roomId,
      tenantId: item.hotel?.id || item.hotelInfo?.hotelId,
      hotelName: item.hotel?.name || item.hotelInfo?.hotelName,
      branchId: '',
      name: item.room?.name || item.roomInfo?.roomName || item.room?.roomType || 'Standard Room',
      roomCategory: item.room?.roomType || item.roomInfo?.roomType || 'Standard Room',
      capacity: item.room?.capacity || item.roomInfo?.capacity || 2,
      bedType: item.room?.bedType,
      amenities: item.room?.amenities,
      specs: item.room?.specs || item.roomInfo?.specs,
      price: item.pricing?.pricePerNight ?? 150,
      currency: item.pricing?.currency || 'USD',
      isActive: true,
      isBookable: true,
      createdAt: ''
    })) || [];
  } catch (error) {
    console.error('Failed to fetch rooms:', error);
    return [];
  }
}

export default function CatalogPage() {
  const { data: rooms, isLoading: roomsLoading, isError: roomsError } = useQuery({
    queryKey: ['available-rooms'],
    queryFn: getAllAvailableRooms
  });

  // Build a map of roomId → first linked room type for price/duration display.
  const roomTypeForRoom = (_roomId: string): RoomTypeResponse | undefined => {
    return undefined; // Will be implemented when we have linked room types
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
