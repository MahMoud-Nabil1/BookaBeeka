import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Loader2, CalendarX2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useRooms } from '../../../catalog/hooks/useInventory';
import { useCreateRoomBlock } from '../../../catalog/hooks/useRoomBlocks';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';

const schema = z.object({
  roomId: z.string().min(1, 'Please select a room'),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().min(1, 'End date is required'),
  reason: z.string().optional(),
}).refine(
  (data) => {
    if (!data.startDate || !data.endDate) return true;
    return new Date(data.startDate) <= new Date(data.endDate);
  },
  {
    message: 'End date must be on or after start date',
    path: ['endDate'],
  }
);

type FormValues = z.infer<typeof schema>;

interface CreateRoomBlockModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function CreateRoomBlockModal({
  open,
  onOpenChange,
}: CreateRoomBlockModalProps) {
  const tenantId = useAppSelector(selectTenantId);
  const { data: rooms, isLoading: loadingRooms } = useRooms(tenantId || undefined);
  const { mutate: createRoomBlock, isPending } = useCreateRoomBlock();

  // Get today's date in YYYY-MM-DD format for min date
  const today = new Date().toISOString().split('T')[0];

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      roomId: '',
      startDate: '',
      endDate: '',
      reason: '',
    },
  });

  const handleSubmit = (values: FormValues) => {
    createRoomBlock(
      {
        roomId: values.roomId,
        startDate: values.startDate,
        endDate: values.endDate,
        reason: values.reason || undefined,
      },
      {
        onSuccess: () => {
          onOpenChange(false);
          form.reset();
        },
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl font-bold">
            <CalendarX2 className="h-5 w-5 text-primary" />
            Block a Room
          </DialogTitle>
          <DialogDescription>
            Prevent bookings for a specific room during a date range. This is useful for
            maintenance, renovations, or other operational needs.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            {/* Room Selection */}
            <FormField
              control={form.control}
              name="roomId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Room</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    value={field.value}
                    disabled={loadingRooms}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select a room to block" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {loadingRooms ? (
                        <div className="py-2 text-center text-sm text-muted-foreground">
                          Loading rooms...
                        </div>
                      ) : rooms && rooms.length > 0 ? (
                        rooms.map((room) => (
                          <SelectItem key={room.id} value={room.id}>
                            {room.name}
                            {room.roomCategory && ` (${room.roomCategory})`}
                          </SelectItem>
                        ))
                      ) : (
                        <div className="py-2 text-center text-sm text-muted-foreground">
                          No rooms available
                        </div>
                      )}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Start Date */}
            <FormField
              control={form.control}
              name="startDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Start Date</FormLabel>
                  <FormControl>
                    <Input
                      type="date"
                      min={today}
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    First date the room will be blocked
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* End Date */}
            <FormField
              control={form.control}
              name="endDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>End Date</FormLabel>
                  <FormControl>
                    <Input
                      type="date"
                      min={form.watch('startDate') || today}
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    Last date the room will be blocked (inclusive)
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Reason */}
            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Reason (Optional)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="e.g., Scheduled maintenance, renovation, repairs..."
                      rows={3}
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    Internal note visible to staff only
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter className="gap-2 sm:gap-0">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isPending || loadingRooms}>
                {isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Creating...
                  </>
                ) : (
                  'Block Room'
                )}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
