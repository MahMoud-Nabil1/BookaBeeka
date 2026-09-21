import { Link } from 'react-router-dom';
import { Users, Bed, ArrowRight } from 'lucide-react';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import type { RoomResponse } from '../../../types/inventory';
import type { RoomTypeResponse } from '../../../types/inventory';

interface RoomCardProps {
  room: RoomResponse;
  /** Primary room type linked to this room (for price/duration display) */
  roomType?: RoomTypeResponse;
}

export default function RoomCard({ room, roomType }: RoomCardProps) {
  return (
    <Card className="overflow-hidden flex flex-col group border-border hover:shadow-raised transition-shadow duration-300">
      {/* Placeholder image banner */}
      <div className="aspect-[16/9] w-full bg-muted flex items-center justify-center relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/10 to-primary/5" />
        <Bed className="relative h-16 w-16 text-primary/20" />
        <Badge className="absolute top-3 right-3 shadow-low bg-background/90 text-foreground hover:bg-background">
          {room.roomCategory}
        </Badge>
      </div>

      <CardHeader className="pb-2">
        <CardTitle className="text-lg line-clamp-1">{room.name}</CardTitle>
      </CardHeader>

      <CardContent className="pb-4 flex-1 space-y-2 text-sm text-muted-foreground">
        {room.capacity > 0 && (
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 shrink-0" />
            <span>Up to {room.capacity} {room.capacity === 1 ? 'guest' : 'guests'}</span>
          </div>
        )}
        {roomType && (
          <div className="flex items-center gap-2">
            <Bed className="h-4 w-4 shrink-0" />
            <span>{roomType.name}</span>
          </div>
        )}
      </CardContent>

      <CardFooter className="pt-0 border-t border-border flex items-center justify-between p-4">
        <div className="font-semibold text-lg text-primary">
          {roomType ? (
            <>
              ${roomType.price.toLocaleString(undefined, { minimumFractionDigits: 2 })}
              <span className="text-sm font-normal text-muted-foreground ml-1">/ night</span>
            </>
          ) : (
            <span className="text-sm font-normal text-muted-foreground">No rate available</span>
          )}
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
