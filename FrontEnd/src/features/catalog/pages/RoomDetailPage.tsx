import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, MapPin, Users, Bed, Loader2, AlertCircle, ShieldCheck, Sparkles, ChevronRight } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import PageLayout from '../../../components/layout/PageLayout';
import { useCreateBooking } from '../../bookings/hooks/useCreateBooking';
import { ProductReviewsSection, StarRating, useRoomReviews } from '../../reviews';
import { useRoomAmenities } from '../hooks/useAmenities';
import { useRoomPhotos } from '../hooks/useMedia';
import api from '../../../config/api';
import { useCatalogSync } from '../../../hooks/useCatalogSync';

const ROOM_TYPE_PHOTOS: Record<string, string> = {
  SUITE: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
  PRESIDENTIAL: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
  DELUXE: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80',
  FAMILY: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=1200&q=80',
  STANDARD: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80',
  ECONOMY: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
};

const DEFAULT_PHOTO = 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80';

function getRoomHeroImage(room: any): string {
  if (!room) return DEFAULT_PHOTO;
  if (room.specs?.imageUrl && typeof room.specs.imageUrl === 'string') return room.specs.imageUrl;
  if (room.specs?.image && typeof room.specs.image === 'string') return room.specs.image;

  const key = (room.roomCategory || room.name || '').toUpperCase();
  for (const [typeKey, url] of Object.entries(ROOM_TYPE_PHOTOS)) {
    if (key.includes(typeKey)) return url;
  }
  return DEFAULT_PHOTO;
}

export default function RoomDetailPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();

  const todayStr = new Date().toISOString().split('T')[0];
  const tomorrowStr = new Date(Date.now() + 86400000).toISOString().split('T')[0];

  const [checkIn, setCheckIn] = useState<string>(todayStr);
  const [checkOut, setCheckOut] = useState<string>(tomorrowStr);

  // Sync catalog updates automatically when a hotel status changes
  useCatalogSync(20000);

  const { data: room, isLoading: roomLoading, isError: roomError } = useQuery({
    queryKey: ['room-detail', roomId],
    queryFn: async () => {
      let found: any = null;

      // Primary: fetch room details directly by roomId
      try {
        const res = await api.get(`/api/availability/rooms/${roomId}`);
        found = res.data;
      } catch {
        // Fallback: search available rooms in case direct lookup fails
        try {
          const res = await api.get('/api/availability/search', {
            params: {
              checkIn: todayStr,
              checkOut: tomorrowStr,
              page: 0,
              size: 100,
            },
          });

          found = res.data.content?.find(
            (item: any) => (item.room?.id || item.roomInfo?.roomId) === roomId
          );
        } catch (searchErr) {
          console.warn('Availability search fallback failed:', searchErr);
        }
      }

      if (!found) {
        throw new Error('Room not found');
      }

      return {
        id: found.room?.id || found.roomInfo?.roomId,
        tenantId: found.hotel?.id || found.hotelInfo?.hotelId,
        hotelName: found.hotel?.name || found.hotelInfo?.hotelName,
        name: found.room?.name || found.roomInfo?.roomName || found.room?.roomType || 'Standard Room',
        roomCategory: found.room?.roomType || found.roomInfo?.roomType || 'Standard Room',
        capacity: found.room?.capacity || found.roomInfo?.capacity || 2,
        bedType: found.room?.bedType || found.room?.specs?.bedType || 'Queen',
        amenities:
          found.room?.amenities && found.room.amenities.length > 0
            ? found.room.amenities
            : found.room?.specs?.amenities || ['Free WiFi', 'Air Conditioning', 'Flat-screen TV'],
        specs: found.room?.specs || found.roomInfo?.specs || {},
        price: found.pricing?.pricePerNight ?? 150,
        currency: found.pricing?.currency || 'USD',
        isBookable: true,
      };
    },
    enabled: !!roomId,
  });

  const { data: reviewsData } = useRoomReviews(room?.id, room?.tenantId);
  const { data: roomAmenities = [] } = useRoomAmenities(room?.id || '');
  const { data: roomPhotos = [] } = useRoomPhotos(room?.id);
  const reviewsList = reviewsData?.content || [];
  const reviewCount = reviewsData?.totalElements || 0;
  const averageRating: number | null =
    reviewsList.length > 0
      ? reviewsList.reduce((acc, r) => acc + (r.rating || 0), 0) / reviewsList.length
      : null;

  // Photo carousel state
  const [currentPhotoIndex, setCurrentPhotoIndex] = useState(0);
  
  // Sort photos by sortOrder and filter for primary first
  const sortedPhotos = [...roomPhotos].sort((a, b) => {
    if (a.isPrimary && !b.isPrimary) return -1;
    if (!a.isPrimary && b.isPrimary) return 1;
    return a.sortOrder - b.sortOrder;
  });

  // Use uploaded photos if available, otherwise fallback to hero image
  const displayPhotos = sortedPhotos.length > 0 
    ? sortedPhotos.map(p => p.url)
    : [getRoomHeroImage(room)];

  const { mutate: createBooking, isPending: isBooking } = useCreateBooking();

  const calculateNights = () => {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    const diff = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
  };

  const nights = calculateNights();
  const pricePerNight = room?.price ?? 150;
  const totalPrice = pricePerNight * nights;

  const handleBookNow = () => {
    if (!room || !room.tenantId || !roomId) return;

    createBooking(
      {
        tenantId: room.tenantId,
        roomId: room.id,
        roomTypeId: '', // room-based booking uses room rate
        start: new Date(`${checkIn}T14:00:00Z`).toISOString(),
        end: new Date(`${checkOut}T11:00:00Z`).toISOString(),
        paymentAmount: totalPrice,
      },
      {
        onSuccess: () => {
          navigate('/portal/bookings');
        },
      }
    );
  };

  if (roomLoading) {
    return (
      <PageLayout>
        <div className="flex justify-center items-center py-24 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin mr-3 text-primary" />
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
          <h2 className="text-2xl font-bold mb-2">Room No Longer Available</h2>
          <p className="text-muted-foreground max-w-md mb-6">
            This room is no longer available. The hotel property may have been suspended, removed, or fully booked.
          </p>
          <Button asChild>
            <Link to="/portal/rooms">Browse Available Rooms</Link>
          </Button>
        </div>
      </PageLayout>
    );
  }

  const roomName = room.name && room.name !== 'ROOM' && room.name !== 'Room'
    ? room.name
    : `${room.roomCategory} Room`;

  const handlePreviousPhoto = () => {
    setCurrentPhotoIndex((prev) => 
      prev === 0 ? displayPhotos.length - 1 : prev - 1
    );
  };

  const handleNextPhoto = () => {
    setCurrentPhotoIndex((prev) => 
      prev === displayPhotos.length - 1 ? 0 : prev + 1
    );
  };

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
          {/* Photo carousel */}
          <div className="rounded-xl overflow-hidden aspect-[21/9] w-full relative shadow-raised group">
            <img
              src={displayPhotos[currentPhotoIndex]}
              alt={`${roomName} - Photo ${currentPhotoIndex + 1}`}
              className="w-full h-full object-cover transition-opacity duration-300"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
            
            {/* Photo navigation */}
            {displayPhotos.length > 1 && (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white opacity-0 group-hover:opacity-100 transition-opacity"
                  onClick={handlePreviousPhoto}
                >
                  <ChevronLeft className="h-6 w-6" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white opacity-0 group-hover:opacity-100 transition-opacity"
                  onClick={handleNextPhoto}
                >
                  <ChevronRight className="h-6 w-6" />
                </Button>
                
                {/* Photo indicators */}
                <div className="absolute bottom-20 left-1/2 -translate-x-1/2 flex gap-2">
                  {displayPhotos.map((_, index) => (
                    <button
                      key={index}
                      onClick={() => setCurrentPhotoIndex(index)}
                      className={`w-2 h-2 rounded-full transition-all ${
                        index === currentPhotoIndex
                          ? 'bg-white w-6'
                          : 'bg-white/50 hover:bg-white/75'
                      }`}
                      aria-label={`View photo ${index + 1}`}
                    />
                  ))}
                </div>
              </>
            )}
            
            {/* Badges */}
            <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between">
              <Badge className="bg-background/90 text-foreground text-sm shadow-low">
                {room.roomCategory}
              </Badge>
              <div className="flex items-center gap-2">
                {sortedPhotos.length > 0 && (
                  <Badge variant="secondary" className="bg-background/90 text-foreground text-sm shadow-low">
                    {currentPhotoIndex + 1} / {displayPhotos.length}
                  </Badge>
                )}
                {room.hotelName && (
                  <Badge variant="secondary" className="bg-background/90 text-foreground text-sm shadow-low">
                    {room.hotelName}
                  </Badge>
                )}
              </div>
            </div>
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-3 mb-2">
              <h1 className="text-3xl font-bold tracking-tight text-foreground">{roomName}</h1>
              {reviewCount > 0 && averageRating !== null && (
                <a
                  href="#reviews-section"
                  className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 text-xs font-semibold hover:bg-amber-500/20 transition-colors cursor-pointer"
                >
                  <StarRating value={Math.round(averageRating)} readonly size="sm" />
                  <span>{averageRating.toFixed(1)}</span>
                  <span className="text-muted-foreground font-normal">({reviewCount} {reviewCount === 1 ? 'review' : 'reviews'})</span>
                </a>
              )}
            </div>

            <p className="text-lg text-muted-foreground mb-6">
              Comfortable accommodation designed for your stay, fully equipped with modern conveniences.
            </p>

            {/* Quick Specs Cards */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
              {room.capacity > 0 && (
                <Card className="shadow-none border-border bg-card">
                  <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                    <Users className="h-6 w-6 text-primary mb-2" />
                    <span className="text-sm font-medium">Capacity</span>
                    <span className="text-sm text-muted-foreground">Up to {room.capacity} {room.capacity === 1 ? 'guest' : 'guests'}</span>
                  </CardContent>
                </Card>
              )}
              {room.bedType && (
                <Card className="shadow-none border-border bg-card">
                  <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                    <Bed className="h-6 w-6 text-primary mb-2" />
                    <span className="text-sm font-medium">Bed Type</span>
                    <span className="text-sm text-muted-foreground">{room.bedType}</span>
                  </CardContent>
                </Card>
              )}
              <Card className="shadow-none border-border bg-card">
                <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                  <MapPin className="h-6 w-6 text-primary mb-2" />
                  <span className="text-sm font-medium">Hotel</span>
                  <span className="text-sm text-muted-foreground truncate w-full text-center">{room.hotelName || 'BookaBeeka Property'}</span>
                </CardContent>
              </Card>
            </div>

            {/* Amenities Section */}
            {roomAmenities.length > 0 && (
              <div className="space-y-3 mb-8">
                <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-primary" /> Included Amenities
                </h2>
                <div className="flex flex-wrap gap-2">
                  {roomAmenities.map((amenity) => (
                    <Badge key={amenity.id} variant="outline" className="px-3 py-1 text-sm bg-muted/30">
                      {amenity.icon && <span className="mr-1">{amenity.icon}</span>}
                      {amenity.name}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Guest Reviews Section */}
            <div id="reviews-section">
              <ProductReviewsSection
                serviceId={room.id}
                tenantId={room.tenantId}
                roomId={room.id}
                roomName={roomName}
              />
            </div>
          </div>
        </div>

        {/* Right Column — Booking Widget */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24 shadow-raised border-border">
            <div className="p-6 border-b border-border rounded-t-xl bg-muted/30">
              <div className="text-3xl font-bold text-primary">
                ${pricePerNight.toFixed(2)}
                <span className="text-lg font-normal text-muted-foreground ml-1">/ night</span>
              </div>
              <p className="text-sm text-muted-foreground mt-1">Best available rate</p>
            </div>

            <CardContent className="p-6 space-y-4">
              {/* Date Pickers */}
              <div className="space-y-3">
                <div>
                  <label className="text-xs font-semibold uppercase text-muted-foreground block mb-1">
                    Check-in Date
                  </label>
                  <div className="relative">
                    <input
                      type="date"
                      value={checkIn}
                      min={todayStr}
                      onChange={(e) => setCheckIn(e.target.value)}
                      className="w-full h-11 px-3 py-2 border border-input rounded-md bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-xs font-semibold uppercase text-muted-foreground block mb-1">
                    Check-out Date
                  </label>
                  <div className="relative">
                    <input
                      type="date"
                      value={checkOut}
                      min={checkIn || todayStr}
                      onChange={(e) => setCheckOut(e.target.value)}
                      className="w-full h-11 px-3 py-2 border border-input rounded-md bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                </div>
              </div>

              {/* Price Breakdown */}
              <div className="pt-4 border-t border-border space-y-2 text-sm">
                <div className="flex justify-between text-muted-foreground">
                  <span>${pricePerNight.toFixed(2)} × {nights} {nights === 1 ? 'night' : 'nights'}</span>
                  <span>${totalPrice.toFixed(2)}</span>
                </div>
                <div className="flex justify-between font-bold text-base text-foreground pt-2 border-t border-border">
                  <span>Total Amount</span>
                  <span className="text-primary">${totalPrice.toFixed(2)}</span>
                </div>
              </div>

              {/* Instant Book Button */}
              <Button
                className="w-full h-12 text-base shadow-low hover:shadow-raised transition-shadow"
                disabled={isBooking || !room.isBookable}
                onClick={handleBookNow}
              >
                {isBooking ? (
                  <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Confirming Reservation...</>
                ) : (
                  `Book Now • $${totalPrice.toFixed(2)}`
                )}
              </Button>

              <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground pt-2">
                <ShieldCheck className="h-4 w-4 text-primary" />
                <span>Instant confirmation & secure checkout</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </PageLayout>
  );
}
