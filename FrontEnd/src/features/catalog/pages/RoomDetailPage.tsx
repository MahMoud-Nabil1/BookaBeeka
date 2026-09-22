import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, MapPin, Users, Bed, Loader2, AlertCircle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import PageLayout from '../../../components/layout/PageLayout';
import SlotPicker from '../components/SlotPicker';
import RoomTypeList from '../components/RoomTypeList';
import { useRoom, useRoomTypes } from '../hooks/useInventory';
import { useCreateBooking } from '../../bookings/hooks/useCreateBooking';
import { useAppSelector } from '../../../redux/hooks';
import { selectTenantId } from '../../../redux/selectors/authSelectors';
import type { SlotDto } from '../../../types/availability';
import type { RoomTypeResponse } from '../../../types/inventory';

export default function RoomDetailPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const tenantId = useAppSelector(selectTenantId);

  const [selectedSlot, setSelectedSlot] = useState<SlotDto | null>(null);
  const [selectedRoomType, setSelectedRoomType] = useState<RoomTypeResponse | null>(null);

  const { data: room, isLoading: roomLoading, isError: roomError } = useRoom(
    roomId,
    tenantId ?? undefined
  );
  const { data: roomTypes = [], isLoading: roomTypesLoading } = useRoomTypes(
    roomId,
    tenantId ?? undefined
  );

  const { mutate: createBooking, isPending: isBooking } = useCreateBooking();

  const handleBookNow = () => {
    if (!selectedSlot || !selectedRoomType || !tenantId || !roomId) return;

    createBooking(
      {
        tenantId,
        roomId,
        roomTypeId: selectedRoomType.id,
        start: selectedSlot.start,
        end: selectedSlot.end,
      },
      { onSuccess: () => navigate('/portal/bookings') }
    );
  };

  if (roomLoading) {
    return (
      <PageLayout>
        <div className="flex justify-center items-center py-24 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin mr-3" />
          <span>Loading room details...</span>
        </div>
      </PageLayout>
    );
  }

  if (roomError || !room) {
    return (
      <PageLayout>
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <AlertCircle className="h-12 w-12 text-destructive mb-4" />
          <h2 className="text-2xl font-bold mb-2">Room Not Found</h2>
          <p className="text-muted-foreground mb-6">The room you are looking for does not exist.</p>
          <Button asChild>
            <Link to="/portal/rooms">Back to Rooms</Link>
          </Button>
        </div>
      </PageLayout>
    );
  }

  const canBook = !!selectedSlot && !!selectedRoomType;

  return (
    <PageLayout
      action={
        <Button variant="ghost" asChild className="gap-2">
          <Link to="/portal/rooms">
            <ChevronLeft className="h-4 w-4" /> Back to Rooms
          </Link>
        </Button>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

        {/* Left Column — Room Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Hero banner */}
          <div className="rounded-xl overflow-hidden aspect-[21/9] w-full bg-muted flex items-center justify-center relative shadow-low">
            <div className="absolute inset-0 bg-gradient-to-br from-primary/10 to-primary/5" />
            <Bed className="relative h-20 w-20 text-primary/20" />
          </div>

          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-3xl font-bold tracking-tight text-foreground">{room.name}</h1>
              <Badge variant="secondary" className="text-sm shadow-low">{room.roomCategory}</Badge>
              {!room.isBookable && (
                <Badge variant="destructive" className="text-sm">Unavailable</Badge>
              )}
            </div>

            <p className="text-lg text-muted-foreground mb-6">
              Comfortable accommodation designed for your stay, fully equipped and ready to book.
            </p>

            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
              {room.capacity > 0 && (
                <Card className="shadow-none border-border">
                  <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                    <Users className="h-6 w-6 text-primary mb-2" />
                    <span className="text-sm font-medium">Capacity</span>
                    <span className="text-sm text-muted-foreground">Up to {room.capacity} {room.capacity === 1 ? 'guest' : 'guests'}</span>
                  </CardContent>
                </Card>
              )}
              {selectedRoomType && (
                <Card className="shadow-none border-border">
                  <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                    <Bed className="h-6 w-6 text-primary mb-2" />
                    <span className="text-sm font-medium">Room Type</span>
                    <span className="text-sm text-muted-foreground">{selectedRoomType.name}</span>
                  </CardContent>
                </Card>
              )}
              <Card className="shadow-none border-border">
                <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                  <MapPin className="h-6 w-6 text-primary mb-2" />
                  <span className="text-sm font-medium">Location</span>
                  <span className="text-sm text-muted-foreground font-mono truncate w-full text-center">{room.branchId.substring(0, 8)}…</span>
                </CardContent>
              </Card>
            </div>

            {/* Room Type selection */}
            <div className="space-y-3">
              <h2 className="text-lg font-semibold text-foreground">Choose Rate Plan</h2>
              {roomTypesLoading ? (
                <div className="flex items-center gap-2 text-muted-foreground py-4">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span className="text-sm">Loading rate plans...</span>
                </div>
              ) : (
                <RoomTypeList
                  roomTypes={roomTypes.filter((rt) => rt.isActive)}
                  selectedRoomTypeId={selectedRoomType?.id}
                  onSelect={setSelectedRoomType}
                />
              )}
            </div>
          </div>
        </div>

        {/* Right Column — Booking Widget */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24 shadow-raised border-border">
            <div className="p-6 border-b border-border rounded-t-xl bg-muted/30">
              {selectedRoomType ? (
                <>
                  <div className="text-3xl font-bold text-primary">
                    ${selectedRoomType.price.toFixed(2)}
                    <span className="text-lg font-normal text-muted-foreground ml-1">/ night</span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">{selectedRoomType.name}</p>
                </>
              ) : (
                <p className="text-muted-foreground text-sm">Select a rate plan above to see pricing</p>
              )}
            </div>

            <CardContent className="p-6">
              {tenantId ? (
                <SlotPicker
                  tenantId={tenantId}
                  resourceId={room.id}
                  selectedSlot={selectedSlot}
                  onSlotSelect={setSelectedSlot}
                />
              ) : (
                <p className="text-sm text-muted-foreground text-center py-4">
                  Tenant information not available.
                </p>
              )}

              <Button
                className="w-full mt-6 h-12 text-base shadow-low"
                disabled={!canBook || isBooking || !room.isBookable}
                onClick={handleBookNow}
              >
                {isBooking ? (
                  <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Confirming...</>
                ) : !selectedRoomType ? (
                  'Select a rate plan'
                ) : !selectedSlot ? (
                  'Select dates'
                ) : (
                  `Book for $${selectedRoomType.price.toFixed(2)}`
                )}
              </Button>

              {!room.isBookable && (
                <p className="text-xs text-destructive text-center mt-3">
                  This room is currently not accepting bookings.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </PageLayout>
  );
}
