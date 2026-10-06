import { useState } from 'react';
import { Check } from 'lucide-react';
import { Button } from '../../../../components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../../../../components/ui/dialog';
import { Badge } from '../../../../components/ui/badge';
import { Checkbox } from '../../../../components/ui/checkbox';
import {
  useAmenities,
  useRoomAmenities,
  useLinkAmenity,
  useUnlinkAmenity,
} from '../../../catalog/hooks/useAmenities';

interface RoomAmenitiesManagerProps {
  roomId: string;
  roomName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function RoomAmenitiesManager({
  roomId,
  roomName,
  open,
  onOpenChange,
}: RoomAmenitiesManagerProps) {
  const { data: allAmenities = [], isLoading: loadingAll } = useAmenities();
  const { data: roomAmenities = [], isLoading: loadingRoom } = useRoomAmenities(roomId);
  const linkAmenity = useLinkAmenity(roomId);
  const unlinkAmenity = useUnlinkAmenity(roomId);

  const [optimisticState, setOptimisticState] = useState<Set<string>>(new Set());

  // Build a set of currently linked amenity IDs
  const linkedIds = new Set(roomAmenities.map((a) => a.id));

  // Merge with optimistic updates
  const effectiveLinkedIds = new Set([...linkedIds, ...optimisticState]);

  const handleToggle = (amenityId: string) => {
    const isCurrentlyLinked = effectiveLinkedIds.has(amenityId);

    if (isCurrentlyLinked) {
      // Unlink
      setOptimisticState((prev) => {
        const next = new Set(prev);
        next.delete(amenityId);
        return next;
      });
      unlinkAmenity.mutate(amenityId, {
        onError: () => {
          // Revert on error
          setOptimisticState((prev) => {
            const next = new Set(prev);
            next.add(amenityId);
            return next;
          });
        },
      });
    } else {
      // Link
      setOptimisticState((prev) => new Set(prev).add(amenityId));
      linkAmenity.mutate(amenityId, {
        onError: () => {
          // Revert on error
          setOptimisticState((prev) => {
            const next = new Set(prev);
            next.delete(amenityId);
            return next;
          });
        },
      });
    }
  };

  const isLoading = loadingAll || loadingRoom;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Manage Room Amenities</DialogTitle>
          <DialogDescription>
            {roomName ? `Select amenities for ${roomName}` : 'Select amenities for this room'}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <div className="py-8 text-center text-muted-foreground">
            Loading amenities...
          </div>
        ) : allAmenities.length === 0 ? (
          <div className="py-8 text-center">
            <p className="text-muted-foreground mb-4">No amenities available</p>
            <p className="text-sm text-muted-foreground">
              Create amenities first in the Amenities management page
            </p>
          </div>
        ) : (
          <div className="space-y-2 max-h-[400px] overflow-y-auto">
            {allAmenities.map((amenity) => {
              const isLinked = effectiveLinkedIds.has(amenity.id);
              return (
                <div
                  key={amenity.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-border hover:bg-muted/50 transition-colors cursor-pointer"
                  onClick={() => handleToggle(amenity.id)}
                >
                  <div className="flex items-center gap-3 flex-1">
                    <Checkbox checked={isLinked} onCheckedChange={() => handleToggle(amenity.id)} />
                    <div className="flex items-center gap-2">
                      {amenity.icon && (
                        <Badge variant="secondary" className="text-sm">
                          {amenity.icon}
                        </Badge>
                      )}
                      <div>
                        <div className="font-medium">{amenity.name}</div>
                        {amenity.description && (
                          <div className="text-xs text-muted-foreground mt-0.5">
                            {amenity.description}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                  {isLinked && (
                    <Check className="h-4 w-4 text-primary flex-shrink-0" />
                  )}
                </div>
              );
            })}
          </div>
        )}

        <div className="flex justify-between items-center pt-4 border-t">
          <div className="text-sm text-muted-foreground">
            {effectiveLinkedIds.size} {effectiveLinkedIds.size === 1 ? 'amenity' : 'amenities'} selected
          </div>
          <Button onClick={() => onOpenChange(false)}>Done</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
