import { Link } from 'react-router-dom';
import { Users, Bed, ArrowRight, Star } from 'lucide-react';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import type { RoomResponse } from '../../../types/inventory';
import type { RoomTypeResponse } from '../../../types/inventory';

interface ExtendedRoom extends RoomResponse {
  price?: number;
  currency?: string;
  hotelName?: string;
  bedType?: string;
  image?: string;
}

interface RoomCardProps {
  room: ExtendedRoom;
  /** Primary room type linked to this room (for price/duration display) */
  roomType?: RoomTypeResponse;
}

const ROOM_TYPE_PHOTOS: Record<string, string> = {
  SUITE: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=600&q=80',
  PRESIDENTIAL: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=600&q=80',
  DELUXE: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=600&q=80',
  FAMILY: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=80',
  STANDARD: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80',
  ECONOMY: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=600&q=80',
};

const DEFAULT_PHOTO = 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=600&q=80';

function getRoomImage(room: ExtendedRoom): string {
  if (room.image) return room.image;
  if (room.specs?.imageUrl && typeof room.specs.imageUrl === 'string') return room.specs.imageUrl;
  if (room.specs?.image && typeof room.specs.image === 'string') return room.specs.image;

  const key = (room.roomCategory || room.name || '').toUpperCase();
  for (const [typeKey, url] of Object.entries(ROOM_TYPE_PHOTOS)) {
    if (key.includes(typeKey)) return url;
  }
  return DEFAULT_PHOTO;
}

export default function RoomCard({ room, roomType }: RoomCardProps) {
  const price = roomType?.price ?? room.price ?? 150;
  const image = getRoomImage(room);
  const roomName = room.name && room.name !== 'ROOM' && room.name !== 'Room'
    ? room.name
    : (room.roomCategory ? `${room.roomCategory.charAt(0).toUpperCase() + room.roomCategory.slice(1).toLowerCase()} Room` : 'Standard Room');

  return (
    <Card className="overflow-hidden flex flex-col group border-border hover:shadow-raised transition-shadow duration-300">
      {/* Room Image Banner */}
      <div className="aspect-[16/9] w-full bg-muted relative overflow-hidden">
        <img
          src={image}
          alt={roomName}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
        <Badge className="absolute top-3 left-3 shadow-low bg-background/90 text-foreground hover:bg-background text-xs">
          {room.roomCategory}
        </Badge>
        {room.hotelName && (
          <Badge variant="secondary" className="absolute top-3 right-3 shadow-low bg-background/90 text-xs">
            {room.hotelName}
          </Badge>
        )}
      </div>

      <CardHeader className="pb-2">
        <div className="flex items-center justify-between gap-2">
          <CardTitle className="text-lg line-clamp-1">{roomName}</CardTitle>
          <div className="flex items-center gap-1 text-xs font-semibold text-amber-500 shrink-0 bg-amber-500/10 px-2 py-0.5 rounded-full">
            <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
            <span>{room.specs?.rating ? Number(room.specs.rating).toFixed(1) : '4.9'}</span>
          </div>
        </div>
      </CardHeader>

      <CardContent className="pb-4 flex-1 space-y-2 text-sm text-muted-foreground">
        {room.capacity > 0 && (
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 shrink-0 text-primary" />
            <span>Up to {room.capacity} {room.capacity === 1 ? 'guest' : 'guests'}</span>
          </div>
        )}
        {room.bedType ? (
          <div className="flex items-center gap-2">
            <Bed className="h-4 w-4 shrink-0 text-primary" />
            <span>{room.bedType}</span>
          </div>
        ) : roomType ? (
          <div className="flex items-center gap-2">
            <Bed className="h-4 w-4 shrink-0 text-primary" />
            <span>{roomType.name}</span>
          </div>
        ) : null}
      </CardContent>

      <CardFooter className="pt-0 border-t border-border flex items-center justify-between p-4 mt-auto">
        <div className="font-semibold text-lg text-primary">
          ${price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          <span className="text-sm font-normal text-muted-foreground ml-1">/ night</span>
        </div>
        <Button asChild size="sm" className="rounded-full shadow-low hover:shadow-raised transition-shadow" disabled={!room.isBookable}>
          <Link to={`/portal/rooms/${room.id}`}>
            Book <ArrowRight className="ml-1 h-4 w-4" />
          </Link>
        </Button>
      </CardFooter>
    </Card>
  );
}
