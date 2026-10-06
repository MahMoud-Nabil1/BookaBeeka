import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, UserCheck, UserX } from 'lucide-react';
import PageLayout from '../../../../components/layout/PageLayout';
import { Button } from '../../../../components/ui/button';
import { Input } from '../../../../components/ui/input';
import { Badge } from '../../../../components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../../../components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '../../../../components/ui/form';
import { ownerApi, type AppointAdminRequest } from '../../api/ownerApi';

// ── Validation schema ────────────────────────────────────────────────────────

const createAdminSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100),
  lastName:  z.string().min(1, 'Last name is required').max(100),
  email:     z.string().email('Invalid email address'),
  password:  z.string().min(6, 'Password must be at least 6 characters'),
  phone:     z.string().optional(),
});

type CreateAdminFormValues = z.infer<typeof createAdminSchema>;

// ── Component ────────────────────────────────────────────────────────────────

export default function OwnerManageAdminsPage() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data: admins = [], isLoading } = useQuery({
    queryKey: ['owner-admins'],
    queryFn: ownerApi.listAdmins,
  });

  const { mutate: createAdmin, isPending, error: createError } = useMutation({
    mutationFn: (req: AppointAdminRequest) => ownerApi.appointAdmin(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['owner-admins'] });
      setDialogOpen(false);
      form.reset();
    },
  });

  const form = useForm<CreateAdminFormValues>({
    resolver: zodResolver(createAdminSchema),
    defaultValues: { firstName: '', lastName: '', email: '', password: '', phone: '' },
  });

  const onSubmit = (values: CreateAdminFormValues) => {
    createAdmin({
      ...values,
      phone: values.phone || undefined,
    });
  };

  const handleOpenChange = (open: boolean) => {
    setDialogOpen(open);
    if (!open) form.reset();
  };

  return (
    <PageLayout
      title="Manage Admins"
      description="Create and view branch admins for your hotel"
    >
      <div className="space-y-6">
        {/* Header row */}
        <div className="flex justify-between items-center">
          <p className="text-sm text-muted-foreground">
            Admins can manage bookings, schedules, rooms, and room blocks.
          </p>
          <Button onClick={() => setDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Admin
          </Button>
        </div>

        {/* Table */}
        {isLoading ? (
          <div className="flex items-center justify-center py-12 text-muted-foreground text-sm">
            Loading admins…
          </div>
        ) : admins.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 border border-dashed rounded-lg gap-3">
            <UserX className="h-10 w-10 text-muted-foreground/50" />
            <p className="text-muted-foreground text-sm">No admins yet</p>
            <Button variant="outline" onClick={() => setDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Create Your First Admin
            </Button>
          </div>
        ) : (
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Phone</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {admins.map((admin) => (
                  <TableRow key={admin.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        <UserCheck className="h-4 w-4 text-muted-foreground" />
                        {admin.firstName} {admin.lastName}
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{admin.email}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {admin.phone ?? <span className="text-muted-foreground/50">—</span>}
                    </TableCell>
                    <TableCell>
                      {admin.isActive ? (
                        <Badge variant="secondary" className="text-green-700 bg-green-50">
                          Active
                        </Badge>
                      ) : (
                        <Badge variant="secondary" className="text-red-700 bg-red-50">
                          Inactive
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {new Date(admin.createdAt).toLocaleDateString()}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {/* Create Admin Dialog */}
      <Dialog open={dialogOpen} onOpenChange={handleOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Create Admin</DialogTitle>
            <DialogDescription>
              Add a new branch admin. They will be able to log in immediately.
            </DialogDescription>
          </DialogHeader>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="firstName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>First Name</FormLabel>
                      <FormControl>
                        <Input placeholder="John" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Last Name</FormLabel>
                      <FormControl>
                        <Input placeholder="Doe" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" placeholder="admin@hotel.com" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Password</FormLabel>
                    <FormControl>
                      <Input type="password" placeholder="Min. 6 characters" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Phone <span className="text-muted-foreground font-normal">(optional)</span></FormLabel>
                    <FormControl>
                      <Input placeholder="+1 555 000 0000" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {createError && (
                <p className="text-sm font-medium text-destructive">
                  Failed to create admin. Please check the details and try again.
                </p>
              )}

              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={isPending}>
                  {isPending ? 'Creating…' : 'Create Admin'}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </PageLayout>
  );
}
