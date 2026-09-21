import { Clock, DollarSign } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { RoomTypeResponse } from '../../../types/inventory';

interface RoomTypeListProps {
  roomTypes: RoomTypeResponse[];
  selectedRoomTypeId?: string;
  onSelect?: (roomType: RoomTypeResponse) => void;
}

export default function RoomTypeList({ roomTypes, selectedRoomTypeId, onSelect }: RoomTypeListProps) {
  if (roomTypes.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4 text-center">
        No room types available for this room.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {roomTypes.map((roomType) => {
        const isSelected = roomType.id === selectedRoomTypeId;
        return (
          <Card
            key={roomType.id}
            onClick={() => onSelect?.(roomType)}
            className={`cursor-pointer transition-all duration-200 border-2 ${
              isSelected
                ? 'border-primary bg-primary/5 shadow-low'
                : 'border-border hover:border-primary/40 hover:shadow-low'
            }`}
          >
            <CardContent className="p-4 flex items-center justify-between gap-4">
              <div className="flex-1 min-w-0">
                <div className="font-medium text-foreground truncate">{roomType.name}</div>
                <div className="flex items-center gap-3 mt-1 text-sm text-muted-foreground flex-wrap">
                  <span className="flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5" />
                    {roomType.durationMinutes} min
                  </span>
                  {roomType.bufferMinutes > 0 && (
                    <span className="text-xs">(+{roomType.bufferMinutes} min buffer)</span>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <Badge variant={isSelected ? 'default' : 'secondary'} className="text-sm font-semibold px-3 py-1">
                  <DollarSign className="h-3.5 w-3.5 mr-0.5" />
                  {roomType.price.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </Badge>
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
