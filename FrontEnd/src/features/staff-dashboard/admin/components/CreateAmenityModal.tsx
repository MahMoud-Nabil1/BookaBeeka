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
import { Input } from '../../../../components/ui/input';
import { Textarea } from '../../../../components/ui/textarea';
import { Button } from '../../../../components/ui/button';
import { useCreateAmenity } from '../../../catalog/hooks/useAmenities';

const createAmenitySchema = z.object({
  name: z.string().min(1, 'Name is required').max(100, 'Name is too long'),
  icon: z.string().max(50, 'Icon is too long').optional(),
  description: z.string().max(500, 'Description is too long').optional(),
});

type CreateAmenityFormData = z.infer<typeof createAmenitySchema>;

interface CreateAmenityModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateAmenityModal({
  open,
  onOpenChange,
}: CreateAmenityModalProps) {
  const createAmenity = useCreateAmenity();

  const form = useForm<CreateAmenityFormData>({
    resolver: zodResolver(createAmenitySchema),
    defaultValues: {
      name: '',
      icon: '',
      description: '',
    },
  });

  const onSubmit = (data: CreateAmenityFormData) => {
    createAmenity.mutate(
      {
        name: data.name,
        icon: data.icon || undefined,
        description: data.description || undefined,
      },
      {
        onSuccess: () => {
          form.reset();
          onOpenChange(false);
        },
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Add New Amenity</DialogTitle>
          <DialogDescription>
            Create a new amenity that can be assigned to rooms
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Name *</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="e.g., Free Wi-Fi, Swimming Pool"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="icon"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Icon (optional)</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g., wifi, pool, parking" {...field} />
                  </FormControl>
                  <FormMessage />
                  <p className="text-xs text-muted-foreground">
                    Enter an icon name or emoji
                  </p>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Description (optional)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Describe this amenity..."
                      rows={3}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="flex justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={createAmenity.isPending}>
                {createAmenity.isPending ? 'Creating...' : 'Create Amenity'}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
