import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  FileText,
  Tag,
  Image as ImageIcon,
  Check,
  Plus,
  Loader2,
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../../../../components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '../../../../components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../../components/ui/select';
import { Input } from '../../../../components/ui/input';
import { Textarea } from '../../../../components/ui/textarea';
import { Button } from '../../../../components/ui/button';
import { useUpdateRoom } from '../../../catalog/hooks/useInventory';
import {
  useAmenities,
  useRoomAmenities,
  useLinkAmenity,
  useUnlinkAmenity,
} from '../../../catalog/hooks/useAmenities';
import { RoomPhotoManager } from './RoomPhotoManager';
import type { RoomResponse } from '../../../../types/inventory';

const ROOM_CATEGORIES = ['STANDARD', 'DELUXE', 'SUITE', 'PENTHOUSE', 'VILLA'] as const;

const schema = z.object({
  name: z.string().min(1, 'Room name is required').max(100, 'Name is too long'),
  roomCategory: z.string().min(1, 'Category is required'),
  capacity: z.coerce.number().int().min(1, 'Capacity must be at least 1').max(50),
  pricePerNight: z.coerce.number().min(0).optional(),
  description: z.string().max(2000, 'Description is too long').optional(),
  isActive: z.boolean(),
  isBookable: z.boolean(),
});

type FormData = z.infer<typeof schema>;

interface Props {
  room: RoomResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditRoomModal({ room, open, onOpenChange }: Props) {
  const updateRoom = useUpdateRoom();
  const [activeTab, setActiveTab] = useState<'details' | 'amenities' | 'photos'>('details');

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: room.name,
      roomCategory: room.roomCategory,
      capacity: room.capacity,
      pricePerNight: (room.specs?.pricePerNight as number) ?? undefined,
      description: (room.specs?.description as string) ?? '',
      isActive: room.isActive,
      isBookable: room.isBookable,
    },
  });

  // Sync form values when the room prop changes
  useEffect(() => {
    form.reset({
      name: room.name,
      roomCategory: room.roomCategory,
      capacity: room.capacity,
      pricePerNight: (room.specs?.pricePerNight as number) ?? undefined,
      description: (room.specs?.description as string) ?? '',
      isActive: room.isActive,
      isBookable: room.isBookable,
    });
    setActiveTab('details');
  }, [room, form]);

  const onSubmit = (data: FormData) => {
    updateRoom.mutate(
      {
        id: room.id,
        req: {
          name: data.name,
          roomCategory: data.roomCategory,
          capacity: data.capacity,
          specs: {
            ...(room.specs || {}),
            ...(data.pricePerNight != null ? { pricePerNight: data.pricePerNight } : {}),
            ...(data.description?.trim() ? { description: data.description.trim() } : {}),
          },
          isActive: data.isActive,
          isBookable: data.isBookable,
        },
      },
      {
        onSuccess: () => onOpenChange(false),
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[620px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Edit Room — {room.name}</DialogTitle>
          <DialogDescription>Update room details, amenities, and photos</DialogDescription>
        </DialogHeader>

        {/* Tab switcher navigation */}
        <div className="flex border-b gap-1 mb-4">
          <button
            type="button"
            onClick={() => setActiveTab('details')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors cursor-pointer ${
              activeTab === 'details'
                ? 'border-primary text-primary font-semibold'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <FileText className="h-4 w-4" />
            Details & Description
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('amenities')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors cursor-pointer ${
              activeTab === 'amenities'
                ? 'border-primary text-primary font-semibold'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <Tag className="h-4 w-4" />
            Amenities
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('photos')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors cursor-pointer ${
              activeTab === 'photos'
                ? 'border-primary text-primary font-semibold'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <ImageIcon className="h-4 w-4" />
            Photos
          </button>
        </div>

        {/* ── TAB 1: Details ── */}
        {activeTab === 'details' && (
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Room Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="e.g., Room 101" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="roomCategory"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Category *</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a category" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {ROOM_CATEGORIES.map((cat) => (
                          <SelectItem key={cat} value={cat}>
                            {cat.charAt(0) + cat.slice(1).toLowerCase()}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="capacity"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Capacity (guests) *</FormLabel>
                      <FormControl>
                        <Input type="number" min={1} max={50} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="pricePerNight"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Price / Night ($)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min={0}
                          step="0.01"
                          placeholder="0.00"
                          {...field}
                          value={field.value ?? ''}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Description */}
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-1.5">
                      <FileText className="h-4 w-4 text-primary" />
                      Room Description
                    </FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Describe room features, view, furnishings..."
                        rows={3}
                        className="resize-none"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="isActive"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Status</FormLabel>
                      <Select
                        onValueChange={(v) => field.onChange(v === 'true')}
                        value={String(field.value)}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="true">Active</SelectItem>
                          <SelectItem value="false">Inactive</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isBookable"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Bookable</FormLabel>
                      <Select
                        onValueChange={(v) => field.onChange(v === 'true')}
                        value={String(field.value)}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="true">Yes</SelectItem>
                          <SelectItem value="false">No</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t">
                <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={updateRoom.isPending}>
                  {updateRoom.isPending ? 'Saving…' : 'Save Changes'}
                </Button>
              </div>
            </form>
          </Form>
        )}

        {/* ── TAB 2: Amenities ── */}
        {activeTab === 'amenities' && (
          <EditRoomAmenitiesTab roomId={room.id} />
        )}

        {/* ── TAB 3: Photos ── */}
        {activeTab === 'photos' && (
          <div className="pt-2">
            <RoomPhotoManager resourceId={room.id} />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

// Inline tab component for managing amenities of an existing room
function EditRoomAmenitiesTab({ roomId }: { roomId: string }) {
  const { data: allAmenities = [], isLoading: loadingAll } = useAmenities();
  const { data: roomAmenities = [], isLoading: loadingRoom } = useRoomAmenities(roomId);
  const linkAmenity = useLinkAmenity(roomId);
  const unlinkAmenity = useUnlinkAmenity(roomId);

  const [optimistic, setOptimistic] = useState<Set<string>>(new Set());

  const linkedIds = new Set(roomAmenities.map((a) => a.id));
  const effectiveLinkedIds = new Set([...linkedIds, ...optimistic]);

  const handleToggle = (amenityId: string) => {
    const isLinked = effectiveLinkedIds.has(amenityId);
    if (isLinked) {
      setOptimistic((prev) => {
        const next = new Set(prev);
        next.delete(amenityId);
        return next;
      });
      unlinkAmenity.mutate(amenityId, {
        onError: () => setOptimistic((prev) => new Set(prev).add(amenityId)),
      });
    } else {
      setOptimistic((prev) => new Set(prev).add(amenityId));
      linkAmenity.mutate(amenityId, {
        onError: () => {
          setOptimistic((prev) => {
            const next = new Set(prev);
            next.delete(amenityId);
            return next;
          });
        },
      });
    }
  };

  if (loadingAll || loadingRoom) {
    return (
      <div className="flex items-center justify-center py-12 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-primary" />
        <span className="text-sm">Loading amenities…</span>
      </div>
    );
  }

  if (allAmenities.length === 0) {
    return (
      <div className="py-8 text-center text-muted-foreground text-sm">
        No amenities available. Create hotel amenities first under the Amenities page.
      </div>
    );
  }

  return (
    <div className="space-y-3 pt-2">
      <p className="text-xs text-muted-foreground">
        Click on any amenity to toggle it on or off for this room.
      </p>
      <div className="flex flex-wrap gap-2 p-2 border rounded-lg bg-muted/20 max-h-[350px] overflow-y-auto">
        {allAmenities.map((amenity) => {
          const isSelected = effectiveLinkedIds.has(amenity.id);
          return (
            <button
              key={amenity.id}
              type="button"
              onClick={() => handleToggle(amenity.id)}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all cursor-pointer border ${
                isSelected
                  ? 'bg-primary text-primary-foreground border-primary shadow-xs'
                  : 'bg-background hover:bg-muted text-foreground border-border'
              }`}
            >
              {isSelected ? (
                <Check className="h-3.5 w-3.5" />
              ) : (
                <Plus className="h-3.5 w-3.5 opacity-60" />
              )}
              <span>{amenity.name}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
