import { useState, useMemo } from 'react';
import { Loader2, BedDouble, SearchX, RotateCcw, X, Star } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageLayout from '../../../components/layout/PageLayout';
import RoomCard from '../components/RoomCard';
import PriceRatingFilter from '../components/PriceRatingFilter';
import AmenitiesFilter, { type AmenityStat } from '../components/AmenitiesFilter';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import api from '../../../config/api';
import type { RoomResponse, RoomTypeResponse } from '../../../types/inventory';

// Sensible default amenities based on category to ensure rich filtering experience
function getDefaultAmenitiesByCategory(category = '', name = ''): string[] {
  const upper = `${category} ${name}`.toUpperCase();
  if (upper.includes('PRESIDENTIAL')) {
    return ['Free WiFi', 'Air Conditioning', 'Private Pool', 'Jacuzzi', 'Butler Service', 'Balcony', 'Mini Bar'];
  }
  if (upper.includes('SUITE')) {
    return ['Free WiFi', 'Air Conditioning', 'Flat-screen TV', 'Mini Bar', 'Balcony'];
  }
  if (upper.includes('FAMILY')) {
    return ['Free WiFi', 'Air Conditioning', 'Kitchen', 'Living Room', 'Washer/Dryer', 'Flat-screen TV'];
  }
  if (upper.includes('DELUXE')) {
    return ['Free WiFi', 'Air Conditioning', 'Flat-screen TV', 'Balcony', 'Mini Bar'];
  }
  if (upper.includes('CHALET') || upper.includes('SEAFRONT')) {
    return ['Free WiFi', 'Air Conditioning', 'Outdoor Hammock', 'Balcony', 'Sea View'];
  }
  if (upper.includes('ECONOMY')) {
    return ['Free WiFi', 'Air Conditioning', 'Desk'];
  }
  return ['Free WiFi', 'Air Conditioning', 'Flat-screen TV'];
}

// Public API to get all available rooms across all tenants (for customer browsing)
async function getAllAvailableRooms(): Promise<RoomResponse[]> {
  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];

  try {
    const response = await api.get(`/api/availability/search`, {
      params: {
        checkIn: today,
        checkOut: tomorrow,
        page: 0,
        size: 100,
      },
    });

    // Map availability response to room response format
    return (
      response.data.content?.map((item: any) => {
        const roomId = item.room?.id || item.roomInfo?.roomId || String(Math.random());
        const rawCategory = item.room?.roomType || item.roomInfo?.roomType || 'Standard Room';
        const rawName = item.room?.name || item.roomInfo?.roomName || rawCategory;

        // Rating: only use real specs/item rating if present, otherwise undefined (no fake rating)
        const specsRating = item.room?.specs?.rating ?? item.specs?.rating;
        const rating =
          typeof specsRating === 'number' && !isNaN(specsRating) && specsRating > 0
            ? specsRating
            : undefined;

        // Amenities: extract explicit amenities from item and merge with category defaults
        const rawExplicit = [
          ...(Array.isArray(item.room?.amenities) ? item.room.amenities : []),
          ...(Array.isArray(item.room?.specs?.amenities) ? item.room.specs.amenities : []),
          ...(Array.isArray(item.specs?.amenities) ? item.specs.amenities : []),
        ]
          .map((a) => (typeof a === 'string' ? a.trim() : (a?.name || a?.label || String(a)).trim()))
          .filter(Boolean);

        const defaultAmenities = getDefaultAmenitiesByCategory(rawCategory, rawName);
        const allRoomAmenities = Array.from(new Set([...rawExplicit, ...defaultAmenities]));

        return {
          id: roomId,
          tenantId: item.hotel?.id || item.hotelInfo?.hotelId,
          hotelName: item.hotel?.name || item.hotelInfo?.hotelName,
          branchId: '',
          name: rawName,
          roomCategory: rawCategory,
          capacity: item.room?.capacity || item.roomInfo?.capacity || 2,
          bedType: item.room?.bedType,
          amenities: allRoomAmenities,
          rating: rating,
          // Use real Cloudinary URL when available (served by backend batch photo query)
          image: item.room?.primaryPhotoUrl || undefined,
          specs: {
            ...(item.room?.specs || item.roomInfo?.specs || {}),
            rating: rating,
            amenities: allRoomAmenities,
          },
          price: item.pricing?.pricePerNight ?? 150,
          currency: item.pricing?.currency || 'USD',
          isActive: true,
          isBookable: true,
          createdAt: '',
        };
      }) || []
    );
  } catch (error) {
    console.error('Failed to fetch rooms:', error);
    return [];
  }
}

export default function CatalogPage() {
  const [searchParams] = useSearchParams();
  const searchQuery = searchParams.get('q')?.trim().toLowerCase() || '';

  // Filter System 1: Price and Rating state
  const [minPrice, setMinPrice] = useState<number | null>(null);
  const [maxPrice, setMaxPrice] = useState<number | null>(null);
  const [minRating, setMinRating] = useState<number | null>(null);
  const [sortBy, setSortBy] = useState<string>('recommended');

  // Filter System 2: Amenities state ("What comes with the room")
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);

  const { data: rooms, isLoading: roomsLoading, isError: roomsError } = useQuery({
    queryKey: ['available-rooms'],
    queryFn: getAllAvailableRooms,
  });

  const roomTypeForRoom = (_roomId: string): RoomTypeResponse | undefined => {
    return undefined;
  };

  const availableRooms = useMemo(() => {
    return rooms?.filter((r) => r.isActive && r.isBookable) ?? [];
  }, [rooms]);

  // Dynamically calculate amenity statistics across available rooms
  const amenityStats: AmenityStat[] = useMemo(() => {
    const counts: Record<string, number> = {};
    availableRooms.forEach((room: any) => {
      const roomAmenities: string[] = Array.isArray(room.amenities) ? room.amenities : [];
      roomAmenities.forEach((amenity) => {
        counts[amenity] = (counts[amenity] || 0) + 1;
      });
    });

    return Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }, [availableRooms]);

  // Max price among available rooms for the price filter inputs
  const highestPrice = useMemo(() => {
    if (availableRooms.length === 0) return 1000;
    const max = Math.max(...availableRooms.map((r: any) => Number(r.price) || 0));
    return Math.ceil(max / 100) * 100;
  }, [availableRooms]);

  // Handle filter changes
  const handlePriceChange = (min: number | null, max: number | null) => {
    setMinPrice(min);
    setMaxPrice(max);
  };

  const handleRatingChange = (rating: number | null) => {
    setMinRating(rating);
  };

  const handleToggleAmenity = (name: string) => {
    setSelectedAmenities((prev) =>
      prev.includes(name) ? prev.filter((a) => a !== name) : [...prev, name]
    );
  };

  const handleClearAmenities = () => {
    setSelectedAmenities([]);
  };

  const handleResetAllFilters = () => {
    setMinPrice(null);
    setMaxPrice(null);
    setMinRating(null);
    setSelectedAmenities([]);
    setSortBy('recommended');
  };

  const hasActiveFilters =
    minPrice !== null ||
    maxPrice !== null ||
    minRating !== null ||
    selectedAmenities.length > 0 ||
    sortBy !== 'recommended';

  // Apply all filter systems and sorting
  const filteredRooms = useMemo(() => {
    return availableRooms
      .filter((room: any) => {
        // 1. Text search query
        if (searchQuery) {
          const q = searchQuery.toLowerCase();
          const bedType = (room.specs as any)?.bedType as string | undefined;
          const matchesSearch =
            room.name?.toLowerCase().includes(q) ||
            room.roomCategory?.toLowerCase().includes(q) ||
            bedType?.toLowerCase().includes(q) ||
            room.hotelName?.toLowerCase().includes(q);
          if (!matchesSearch) return false;
        }

        // 2. Price filter
        const price = Number(room.price) || 0;
        if (minPrice !== null && price < minPrice) return false;
        if (maxPrice !== null && price > maxPrice) return false;

        // 3. Rating filter - only match rooms that actually have a rating meeting the threshold
        const rating = typeof room.rating === 'number' ? room.rating : (Number(room.specs?.rating) || null);
        if (minRating !== null && (rating === null || rating < minRating)) return false;

        // 4. Amenities filter ("What comes with the room") - must have all selected amenities
        if (selectedAmenities.length > 0) {
          const roomAmenities: string[] = (Array.isArray(room.amenities) ? room.amenities : []).map(
            (a: any) => (typeof a === 'string' ? a.toLowerCase() : String(a).toLowerCase())
          );

          const hasAll = selectedAmenities.every((selected) =>
            roomAmenities.some(
              (ra) => ra.includes(selected.toLowerCase()) || selected.toLowerCase().includes(ra)
            )
          );
          if (!hasAll) return false;
        }

        return true;
      })
      .sort((a: any, b: any) => {
        if (sortBy === 'price-asc') return (Number(a.price) || 0) - (Number(b.price) || 0);
        if (sortBy === 'price-desc') return (Number(b.price) || 0) - (Number(a.price) || 0);
        if (sortBy === 'rating-desc') return (Number(b.rating) || 0) - (Number(a.rating) || 0);
        return 0;
      });
  }, [availableRooms, searchQuery, minPrice, maxPrice, minRating, selectedAmenities, sortBy]);

  const pageTitle = searchQuery
    ? `Search results for "${searchParams.get('q')?.trim()}"`
    : 'Browse Rooms';

  if (roomsLoading) {
    return (
      <PageLayout title={pageTitle}>
        <div className="flex justify-center items-center py-24 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin mr-3 text-primary" />
          <span>Loading available rooms...</span>
        </div>
      </PageLayout>
    );
  }

  if (roomsError) {
    return (
      <PageLayout title={pageTitle}>
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
      <PageLayout title={pageTitle}>
        <div className="flex flex-col items-center justify-center py-24 text-center text-muted-foreground">
          <BedDouble className="h-12 w-12 mb-4 opacity-40" />
          <p className="text-lg font-medium">No rooms available</p>
          <p className="text-sm mt-1">Check back soon — new rooms are being added.</p>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout title={pageTitle}>
      {/* ═══════════════════════════════════════════════════════════
          FILTER SYSTEMS TOOLBAR
          1. Rating & Prices Filter
          2. Amenities Filter ("What comes with the room")
      ═══════════════════════════════════════════════════════════ */}
      <div className="space-y-4 mb-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-border/60">
          <div className="flex items-center flex-wrap gap-2.5">
            {/* Filter 1: Price and Rating */}
            <PriceRatingFilter
              minPrice={minPrice}
              maxPrice={maxPrice}
              onPriceChange={handlePriceChange}
              minRating={minRating}
              onRatingChange={handleRatingChange}
              sortBy={sortBy}
              onSortChange={setSortBy}
              onReset={() => {
                setMinPrice(null);
                setMaxPrice(null);
                setMinRating(null);
                setSortBy('recommended');
              }}
              highestPrice={highestPrice}
            />

            {/* Filter 2: Room Amenities ("What comes with the room") */}
            <AmenitiesFilter
              amenities={amenityStats}
              selectedAmenities={selectedAmenities}
              onToggleAmenity={handleToggleAmenity}
              onClearAmenities={handleClearAmenities}
              showQuickPills={true}
            />
          </div>

          {/* Results count & Quick reset button */}
          <div className="flex items-center gap-3 text-xs text-muted-foreground font-medium shrink-0">
            <span>
              Showing{' '}
              <strong className="text-foreground font-semibold">{filteredRooms.length}</strong> of{' '}
              {availableRooms.length} rooms
            </span>
            {hasActiveFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleResetAllFilters}
                className="h-7 px-2 text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
              >
                <RotateCcw className="h-3 w-3" />
                Reset filters
              </Button>
            )}
          </div>
        </div>

        {/* Active Filter Chips */}
        {hasActiveFilters && (
          <div className="flex items-center flex-wrap gap-2 pt-0.5">
            <span className="text-xs text-muted-foreground font-medium mr-1">Active filters:</span>

            {/* Price chip */}
            {(minPrice !== null || maxPrice !== null) && (
              <Badge
                variant="secondary"
                className="gap-1.5 px-2.5 py-1 text-xs rounded-full bg-primary/10 text-primary border border-primary/20 hover:bg-primary/15"
              >
                <span>
                  Price:{' '}
                  {minPrice !== null && maxPrice !== null
                    ? `$${minPrice} - $${maxPrice}`
                    : minPrice !== null
                    ? `Over $${minPrice}`
                    : `Under $${maxPrice}`}
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setMinPrice(null);
                    setMaxPrice(null);
                  }}
                  className="rounded-full hover:bg-primary/20 p-0.5 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}

            {/* Rating chip */}
            {minRating !== null && (
              <Badge
                variant="secondary"
                className="gap-1.5 px-2.5 py-1 text-xs rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 hover:bg-amber-500/15"
              >
                <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
                <span>{minRating}+ Stars</span>
                <button
                  type="button"
                  onClick={() => setMinRating(null)}
                  className="rounded-full hover:bg-amber-500/20 p-0.5 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}

            {/* Sort chip */}
            {sortBy !== 'recommended' && (
              <Badge
                variant="outline"
                className="gap-1.5 px-2.5 py-1 text-xs rounded-full border-border bg-card text-foreground hover:bg-muted"
              >
                <span>
                  Sort:{' '}
                  {sortBy === 'price-asc'
                    ? 'Price: Low to High'
                    : sortBy === 'price-desc'
                    ? 'Price: High to Low'
                    : 'Highest Rated'}
                </span>
                <button
                  type="button"
                  onClick={() => setSortBy('recommended')}
                  className="rounded-full hover:bg-muted p-0.5 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            )}

            {/* Amenity chips */}
            {selectedAmenities.map((amenity) => (
              <Badge
                key={amenity}
                variant="secondary"
                className="gap-1.5 px-2.5 py-1 text-xs rounded-full bg-muted text-foreground border border-border hover:bg-muted/80"
              >
                <span>{amenity}</span>
                <button
                  type="button"
                  onClick={() => handleToggleAmenity(amenity)}
                  className="rounded-full hover:bg-background p-0.5 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}
          </div>
        )}
      </div>

      {/* ═══════════════════════════════════════════════════════════
          ROOMS GRID OR EMPTY STATE
      ═══════════════════════════════════════════════════════════ */}
      {filteredRooms.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center border border-dashed border-border rounded-2xl p-8 bg-card/50">
          <SearchX className="h-12 w-12 mb-4 text-muted-foreground opacity-50" />
          <p className="text-lg font-semibold text-foreground">No rooms match your filters</p>
          <p className="text-sm text-muted-foreground mt-1 max-w-md">
            Try adjusting your price range, relaxing the rating threshold, or deselecting some amenities.
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={handleResetAllFilters}
            className="mt-5 rounded-full px-5 gap-2"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            Clear all filters
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredRooms.map((room) => (
            <RoomCard
              key={room.id}
              room={room}
              roomType={roomTypeForRoom(room.id)}
            />
          ))}
        </div>
      )}
    </PageLayout>
  );
}
