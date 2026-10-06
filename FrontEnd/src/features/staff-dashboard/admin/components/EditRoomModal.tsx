import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
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
import { Button } from '../../../../components/ui/button';
import { useUpdateRoom } from '../../../catalog/hooks/useInventory';
import type { RoomResponse } from '../../../../types/inventory';

const ROOM_CATEGORIES = ['STANDARD', 'DELUXE', 'SUITE', 'PENTHOUSE', 'VILLA'] as const;

const schema = z.object({
  name: z.string().min(1, 'Room name is required').max(100, 'Name is too long'),
  roomCategory: z.string().min(1, 'Category is required'),
  capacity: z.coerce.number().int().min(1, 'Capacity must be at least 1').max(50),
  pricePerNight: z.coerce.number().min(0).optional(),
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

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: room.name,
      roomCategory: room.roomCategory,
      capacity: room.capacity,
      pricePerNight: (room.specs?.pricePerNight as number) ?? undefined,
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
      isActive: room.isActive,
      isBookable: room.isBookable,
    });
  }, [room, form]);

  const onSubmit = (data: FormData) => {
    updateRoom.mutate(
      {
        id: room.id,
        req: {
          name: data.name,
          roomCategory: data.roomCategory,
          capacity: data.capacity,
          specs: data.pricePerNight != null ? { pricePerNight: data.pricePerNight } : undefined,
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
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle>Edit Room</DialogTitle>
          <DialogDescription>Update details for {room.name}</DialogDescription>
        </DialogHeader>

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
                    <FormLabel>Price / Night</FormLabel>
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

            <div className="flex justify-end gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={updateRoom.isPending}>
                {updateRoom.isPending ? 'Saving…' : 'Save Changes'}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
