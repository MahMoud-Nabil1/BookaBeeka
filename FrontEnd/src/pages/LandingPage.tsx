import { Link } from 'react-router-dom';
import { ArrowRight, CalendarCheck, BedDouble, Shield, Hotel, Users, Bed, Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import Navbar from '../components/layout/Navbar';
import api from '../config/api';
import { useAppSelector } from '../redux/hooks';
import { selectIsAuthenticated, selectUserType } from '../redux/selectors/authSelectors';

interface AvailableRoomItem {
  hotel: {
    id: string;
    name: string;
    subdomain?: string;
  };
  room: {
    id: string;
    name?: string;
    roomType?: string;
    capacity?: number;
    bedType?: string;
    amenities?: string[];
    specs?: Record<string, any>;
  };
  stay?: {
    checkIn: string;
    checkOut: string;
    nights: number;
  };
  pricing?: {
    pricePerNight?: number;
    totalPrice?: number;
    currency?: string;
  };
}

const ROOM_TYPE_PHOTOS: Record<string, string> = {
  SUITE: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=600&q=80',
  PRESIDENTIAL: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=600&q=80',
  DELUXE: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=600&q=80',
  FAMILY: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=80',
  STANDARD: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80',
  ECONOMY: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=600&q=80',
};

const FALLBACK_PHOTOS = [
  'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=600&q=80',
];

function getRoomImage(room: AvailableRoomItem['room'], index: number): string {
  if (room.specs?.imageUrl && typeof room.specs.imageUrl === 'string') return room.specs.imageUrl;
  if (room.specs?.image && typeof room.specs.image === 'string') return room.specs.image;

  const key = (room.roomType || room.name || '').toUpperCase();
  for (const [typeKey, url] of Object.entries(ROOM_TYPE_PHOTOS)) {
    if (key.includes(typeKey)) return url;
  }
  return FALLBACK_PHOTOS[index % FALLBACK_PHOTOS.length];
}

function getRoomDescription(room: AvailableRoomItem['room']): string {
  if (room.specs?.description && typeof room.specs.description === 'string') {
    return room.specs.description;
  }
  if (room.amenities && room.amenities.length > 0) {
    return room.amenities.slice(0, 3).join(' • ') + (room.specs?.view ? ` • ${room.specs.view}` : '');
  }
  if (room.specs?.view) {
    return `Enjoy picturesque ${room.specs.view.toString().toLowerCase()} with refined furnishings.`;
  }
  return `Spacious ${room.bedType || room.roomType || 'room'} featuring modern essentials and premium comfort.`;
}

const STEPS = [
  {
    icon: Hotel,
    title: 'Browse Rooms',
    description: 'Explore our curated selection of hotel rooms and accommodations.',
  },
  {
    icon: CalendarCheck,
    title: 'Pick Your Dates',
    description: 'Choose your check-in and check-out dates that fit your schedule.',
  },
  {
    icon: Shield,
    title: 'Book Instantly',
    description: 'Secure your room with a confirmed reservation in seconds.',
  },
];

async function fetchFeaturedRooms(): Promise<AvailableRoomItem[]> {
  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];

  const response = await api.get('/api/availability/search', {
    params: {
      checkIn: today,
      checkOut: tomorrow,
      page: 0,
      size: 8,
    },
  });

  return response.data.content || [];
}

export default function LandingPage() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const userType = useAppSelector(selectUserType);

  const { data: featuredRooms, isLoading, isError } = useQuery({
    queryKey: ['landing-featured-rooms'],
    queryFn: fetchFeaturedRooms,
  });

  const getBookingLink = (roomId: string) => {
    if (!isAuthenticated) return '/login/customer';
    if (userType === 'STAFF') return '/staff';
    return `/portal/rooms/${roomId}`;
  };

  const getBookingBtnText = () => {
    if (!isAuthenticated) return 'Book Now';
    if (userType === 'STAFF') return 'Staff View';
    return 'Book Now';
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Navbar />

      {/* ── Hero ──────────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        {/* Subtle background decoration */}
        <div className="absolute inset-0 -z-10">
          <div className="absolute top-[-20%] right-[-10%] w-[600px] h-[600px] rounded-full bg-primary/5 blur-3xl" />
          <div className="absolute bottom-[-30%] left-[-10%] w-[500px] h-[500px] rounded-full bg-primary/3 blur-3xl" />
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24 sm:pt-28 sm:pb-32">
          <div className="max-w-3xl">
            <Badge variant="secondary" className="mb-6 px-4 py-1.5 text-sm font-medium shadow-low">
              <BedDouble className="mr-1.5 h-3.5 w-3.5" />
              Book your perfect stay
            </Badge>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-foreground leading-[1.1] mb-6">
              Find your ideal
              <span className="text-primary"> hotel room</span>
            </h1>

            <p className="text-lg sm:text-xl text-muted-foreground max-w-2xl mb-10 leading-relaxed">
              BookaBeeka makes it effortless to discover, book, and manage your hotel reservations.
              From budget rooms to luxury suites — find the perfect accommodation for your stay.
            </p>

            {/* Dynamic Hero CTAs based on auth state */}
            <div className="flex flex-col sm:flex-row items-start gap-4">
              {!isAuthenticated ? (
                <>
                  <Button size="lg" className="h-13 px-8 text-base shadow-raised hover:shadow-overlay transition-shadow" asChild>
                    <Link to="/register">
                      Get Started Free
                      <ArrowRight className="ml-2 h-5 w-5" />
                    </Link>
                  </Button>
                  <Button size="lg" variant="outline" className="h-13 px-8 text-base" asChild>
                    <Link to="/login/customer">
                      Sign In
                    </Link>
                  </Button>
                </>
              ) : userType === 'CUSTOMER' ? (
                <>
                  <Button size="lg" className="h-13 px-8 text-base shadow-raised hover:shadow-overlay transition-shadow" asChild>
                    <Link to="/portal/rooms">
                      Browse Rooms
                      <ArrowRight className="ml-2 h-5 w-5" />
                    </Link>
                  </Button>
                  <Button size="lg" variant="outline" className="h-13 px-8 text-base" asChild>
                    <Link to="/portal/bookings">
                      My Bookings
                    </Link>
                  </Button>
                </>
              ) : (
                <Button size="lg" className="h-13 px-8 text-base shadow-raised hover:shadow-overlay transition-shadow" asChild>
                  <Link to="/staff">
                    Go to Staff Portal
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </Link>
                </Button>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* ── Featured Rooms ────────────────────────────────────── */}
      <section className="bg-muted/40 border-y border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-24">
          <div className="text-center mb-14">
            <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-foreground mb-4">
              Featured accommodations
            </h2>
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
              From cozy standard rooms to spacious suites — discover the perfect place for your stay.
            </p>
          </div>

          {isLoading ? (
            <div className="flex justify-center items-center py-20 text-muted-foreground">
              <Loader2 className="h-8 w-8 animate-spin mr-3 text-primary" />
              <span className="text-lg">Loading featured accommodations...</span>
            </div>
          ) : isError ? (
            <div className="text-center py-16 text-muted-foreground">
              <BedDouble className="h-12 w-12 mx-auto mb-3 opacity-40" />
              <p className="text-base font-medium">Unable to load accommodations at this time.</p>
            </div>
          ) : !featuredRooms || featuredRooms.length === 0 ? (
            <div className="text-center py-16 text-muted-foreground">
              <BedDouble className="h-12 w-12 mx-auto mb-3 opacity-40" />
              <p className="text-base font-medium">No accommodations available right now.</p>
              <p className="text-sm mt-1">Check back soon as new rooms are added regularly.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {featuredRooms.map((item, index) => {
                const roomName = item.room?.name || item.room?.roomType || 'Standard Room';
                const roomType = item.room?.roomType || 'Room';
                const price = item.pricing?.pricePerNight ?? 150;
                const image = getRoomImage(item.room, index);
                const description = getRoomDescription(item.room);
                const hotelName = item.hotel?.name;

                return (
                  <Card
                    key={item.room?.id || index}
                    className="group overflow-hidden border-border bg-card hover:shadow-raised transition-shadow duration-300 flex flex-col"
                  >
                    <div className="aspect-[4/3] w-full overflow-hidden relative">
                      <img
                        src={image}
                        alt={roomName}
                        className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                      />
                      <Badge className="absolute top-3 left-3 bg-background/90 text-foreground hover:bg-background shadow-low text-xs">
                        {roomType}
                      </Badge>
                      {hotelName && (
                        <Badge variant="secondary" className="absolute top-3 right-3 bg-background/90 text-xs shadow-low">
                          {hotelName}
                        </Badge>
                      )}
                    </div>
                    <CardContent className="p-5 flex-1 flex flex-col justify-between">
                      <div>
                        <h3 className="font-semibold text-lg mb-1 text-foreground line-clamp-1">{roomName}</h3>
                        <p className="text-sm text-muted-foreground mb-3 line-clamp-2">{description}</p>
                        
                        {item.room?.capacity ? (
                          <div className="flex items-center gap-1.5 text-xs text-muted-foreground mb-4">
                            <Users className="h-3.5 w-3.5 text-primary" />
                            <span>Up to {item.room.capacity} {item.room.capacity === 1 ? 'guest' : 'guests'}</span>
                            {item.room.bedType && (
                              <>
                                <span>•</span>
                                <Bed className="h-3.5 w-3.5 text-primary ml-1" />
                                <span>{item.room.bedType}</span>
                              </>
                            )}
                          </div>
                        ) : null}
                      </div>

                      <div className="flex items-center justify-between pt-3 border-t border-border mt-auto">
                        <div>
                          <span className="text-xl font-bold text-primary">${price}</span>
                          <span className="text-sm text-muted-foreground ml-1">/ night</span>
                        </div>
                        <Button size="sm" variant="ghost" className="text-primary hover:text-primary font-medium" asChild>
                          <Link to={getBookingLink(item.room?.id)}>
                            {getBookingBtnText()} <ArrowRight className="ml-1 h-4 w-4" />
                          </Link>
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      </section>

      {/* ── How It Works ──────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-24">
        <div className="text-center mb-14">
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-foreground mb-4">
            How it works
          </h2>
          <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
            Three simple steps to your next hotel booking.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 lg:gap-12">
          {STEPS.map((step, i) => (
            <div key={step.title} className="text-center group">
              <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                <step.icon className="h-7 w-7" />
              </div>
              <div className="text-sm font-semibold text-primary mb-2">Step {i + 1}</div>
              <h3 className="text-xl font-semibold text-foreground mb-2">{step.title}</h3>
              <p className="text-muted-foreground leading-relaxed">{step.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA Banner ────────────────────────────────────────── */}
      <section className="border-t border-border bg-foreground text-background">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-20 flex flex-col sm:flex-row items-center justify-between gap-8">
          <div>
            <h2 className="text-2xl sm:text-3xl font-bold mb-2">
              {!isAuthenticated
                ? 'Ready to book your stay?'
                : userType === 'CUSTOMER'
                ? 'Ready for your next getaway?'
                : 'Hotel Management Portal'}
            </h2>
            <p className="text-background/70 text-lg">
              {!isAuthenticated
                ? 'Create your account and reserve your room today.'
                : userType === 'CUSTOMER'
                ? 'Browse all available rooms and manage your reservations effortlessly.'
                : 'Access your staff dashboard, oversee bookings, and manage hotel inventory.'}
            </p>
          </div>
          <div className="flex gap-4 shrink-0">
            {!isAuthenticated ? (
              <>
                <Button size="lg" className="h-12 px-8 shadow-raised" asChild>
                  <Link to="/register">Sign Up Free</Link>
                </Button>
                <Button size="lg" variant="outline" className="h-12 px-8 border-background/20 text-background hover:bg-background/10 hover:text-background" asChild>
                  <Link to="/login/owner">Owner Portal</Link>
                </Button>
                <Button size="lg" variant="outline" className="h-12 px-6 border-background/20 text-background hover:bg-background/10 hover:text-background" asChild>
                  <Link to="/login/superadmin">SuperAdmin</Link>
                </Button>
              </>
            ) : userType === 'CUSTOMER' ? (
              <>
                <Button size="lg" className="h-12 px-8 shadow-raised" asChild>
                  <Link to="/portal/rooms">Browse All Rooms</Link>
                </Button>
                <Button size="lg" variant="outline" className="h-12 px-8 border-background/20 text-background hover:bg-background/10 hover:text-background" asChild>
                  <Link to="/portal/bookings">My Bookings</Link>
                </Button>
              </>
            ) : (
              <Button size="lg" className="h-12 px-8 shadow-raised" asChild>
                <Link to="/staff">Go to Staff Dashboard</Link>
              </Button>
            )}
          </div>
        </div>
      </section>

      {/* ── Footer ────────────────────────────────────────────── */}
      <footer className="border-t border-border bg-muted/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary">
              <span className="font-bold text-white text-sm leading-none">B</span>
            </div>
            <span className="font-semibold text-foreground">BookaBeeka</span>
          </Link>
          <p className="text-sm text-muted-foreground">
            © {new Date().getFullYear()} BookaBeeka. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
