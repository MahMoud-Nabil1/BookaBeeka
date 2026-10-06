import { useEffect } from 'react';
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
import { useUpdateRoomBlock } from '../../../catalog/hooks/useRoomBlocks';
import type { RoomBlockResponse } from '../../../../types/availability';

const schema = z.object({
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

interface EditRoomBlockModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  roomBlock: RoomBlockResponse | null;
}

export default function EditRoomBlockModal({
  open,
  onOpenChange,
  roomBlock,
}: EditRoomBlockModalProps) {
  const { mutate: updateRoomBlock, isPending } = useUpdateRoomBlock();

  // Get today's date in YYYY-MM-DD format for min date
  const today = new Date().toISOString().split('T')[0];

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      startDate: '',
      endDate: '',
      reason: '',
    },
  });

  // Pre-fill form when roomBlock changes
  useEffect(() => {
    if (roomBlock) {
      form.reset({
        startDate: roomBlock.startDate,
        endDate: roomBlock.endDate,
        reason: roomBlock.reason || '',
      });
    }
  }, [roomBlock, form]);

  const handleSubmit = (values: FormValues) => {
    if (!roomBlock) return;

    updateRoomBlock(
      {
        id: roomBlock.id,
        request: {
          startDate: values.startDate,
          endDate: values.endDate,
          reason: values.reason || undefined,
        },
      },
      {
        onSuccess: () => {
          onOpenChange(false);
        },
      }
    );
  };

  if (!roomBlock) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl font-bold">
            <CalendarX2 className="h-5 w-5 text-primary" />
            Edit Room Block
          </DialogTitle>
          <DialogDescription>
            Update the dates or reason for this room block.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            {/* Display room name (read-only) */}
            <div className="rounded-md bg-muted px-3 py-2">
              <div className="text-xs font-semibold uppercase text-muted-foreground">
                Room
              </div>
              <div className="text-sm font-medium mt-1">
                Room ID: {roomBlock.roomId}
              </div>
            </div>

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
              <Button type="submit" disabled={isPending}>
                {isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : (
                  'Update Room Block'
                )}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
