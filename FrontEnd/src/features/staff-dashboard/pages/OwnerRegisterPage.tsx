import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { Building2, ChevronDown } from 'lucide-react';
import { ownerApi } from '../api/ownerApi';
import type { OwnerRegisterRequest } from '../../../types/auth';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';

const registerSchema = z.object({
  // Hotel info
  hotelName: z.string().min(1, 'Hotel name is required'),
  subdomain: z
    .string()
    .min(1, 'Subdomain is required')
    .regex(/^[a-z0-9-]+$/, 'Only lowercase letters, numbers and hyphens'),
  // Owner info
  firstName: z.string().min(1, 'First name is required'),
  lastName: z.string().min(1, 'Last name is required'),
  email: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  // Optional
  phone: z.string().optional(),
  currency: z.string().optional(),
  timezone: z.string().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function OwnerRegisterPage() {
  const navigate = useNavigate();

  const { mutate: register, isPending, error, isSuccess } = useMutation({
    mutationFn: (req: OwnerRegisterRequest) => ownerApi.registerOwner(req),
    onSuccess: () => {
      // Short delay so the success message is visible before redirecting
      setTimeout(() => navigate('/login/owner'), 2000);
    },
  });

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      hotelName: '',
      subdomain: '',
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
      phone: '',
      currency: '',
      timezone: '',
    },
  });

  // Auto-generate subdomain from hotel name
  const handleHotelNameChange = (value: string) => {
    const current = form.getValues('subdomain');
    if (!current) {
      const generated = value
        .toLowerCase()
        .replace(/\s+/g, '-')
        .replace(/[^a-z0-9-]/g, '');
      form.setValue('subdomain', generated, { shouldValidate: false });
    }
  };

  const onSubmit = (values: RegisterFormValues) => {
    const { confirmPassword, ...req } = values;
    register({
      ...req,
      phone: req.phone || undefined,
      currency: req.currency || undefined,
      timezone: req.timezone || undefined,
    });
  };

  const apiError = error as any;
  const errorMessage =
    apiError?.response?.data?.message ||
    apiError?.response?.data ||
    (typeof apiError?.message === 'string' ? apiError.message : null) ||
    'Something went wrong. Please try again.';

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-muted/50">
      <Card className="w-full max-w-lg">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-2">
            <div className="rounded-full bg-primary/10 p-3">
              <Building2 className="h-6 w-6 text-primary" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight">Create Owner Account</CardTitle>
          <CardDescription>Register your hotel and get started with BookaBeeka</CardDescription>
        </CardHeader>

        <CardContent>
          {isSuccess ? (
            <div className="rounded-md bg-green-50 border border-green-200 p-4 text-center space-y-1">
              <p className="text-sm font-medium text-green-800">Account created successfully!</p>
              <p className="text-xs text-green-600">Redirecting you to the login page…</p>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">

                {/* ── Hotel Information ─────────────────────────── */}
                <div className="space-y-3">
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Hotel Information
                  </p>

                  <FormField
                    control={form.control}
                    name="hotelName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Hotel Name</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="Grand Palace Hotel"
                            {...field}
                            onChange={(e) => {
                              field.onChange(e);
                              handleHotelNameChange(e.target.value);
                            }}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="subdomain"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Subdomain</FormLabel>
                        <FormControl>
                          <div className="flex items-center rounded-md border border-input bg-background ring-offset-background focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2">
                            <input
                              {...field}
                              placeholder="grand-palace"
                              className="flex-1 bg-transparent px-3 py-2 text-sm outline-none placeholder:text-muted-foreground"
                            />
                            <span className="border-l border-input px-3 py-2 text-sm text-muted-foreground bg-muted/50 rounded-r-md select-none">
                              .bookabeeka.com
                            </span>
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                {/* ── Owner Information ─────────────────────────── */}
                <div className="space-y-3">
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Owner Information
                  </p>

                  <div className="grid grid-cols-2 gap-3">
                    <FormField
                      control={form.control}
                      name="firstName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>First Name</FormLabel>
                          <FormControl>
                            <Input placeholder="Jane" {...field} />
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
                            <Input placeholder="Smith" {...field} />
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
                          <Input type="email" placeholder="jane@grandpalace.com" {...field} />
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
                          <Input type="password" placeholder="••••••••" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="confirmPassword"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Confirm Password</FormLabel>
                        <FormControl>
                          <Input type="password" placeholder="••••••••" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                {/* ── Optional Settings (collapsed by default) ──── */}
                <Collapsible>
                  <CollapsibleTrigger asChild>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="w-full flex items-center justify-between text-muted-foreground hover:text-foreground -mx-1 px-1"
                    >
                      <span className="text-xs font-semibold uppercase tracking-wider">
                        Optional Settings
                      </span>
                      <ChevronDown className="h-4 w-4 transition-transform duration-200 [[data-state=open]_&]:rotate-180" />
                    </Button>
                  </CollapsibleTrigger>
                  <CollapsibleContent className="space-y-3 pt-2">
                    <FormField
                      control={form.control}
                      name="phone"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Phone</FormLabel>
                          <FormControl>
                            <Input type="tel" placeholder="+1 555 000 0000" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <div className="grid grid-cols-2 gap-3">
                      <FormField
                        control={form.control}
                        name="currency"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Currency</FormLabel>
                            <FormControl>
                              <Input placeholder="USD" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="timezone"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Timezone</FormLabel>
                            <FormControl>
                              <Input placeholder="UTC" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </CollapsibleContent>
                </Collapsible>

                {error && (
                  <div className="rounded-md bg-destructive/10 border border-destructive/20 px-3 py-2 text-sm text-destructive">
                    {errorMessage}
                  </div>
                )}

                <Button type="submit" className="w-full" disabled={isPending}>
                  {isPending ? 'Creating account…' : 'Create Account'}
                </Button>
              </form>
            </Form>
          )}
        </CardContent>

        <CardFooter className="flex flex-col space-y-2 text-sm text-center border-t border-border pt-4">
          <div className="text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login/owner" className="hover:underline text-primary font-medium">
              Sign in
            </Link>
          </div>
          <div className="text-muted-foreground">
            Customer?{' '}
            <Link to="/login/customer" className="hover:underline text-primary">
              Customer Login
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
