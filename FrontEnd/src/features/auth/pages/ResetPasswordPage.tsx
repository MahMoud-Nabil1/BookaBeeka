import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useResetPassword } from '../hooks/usePasswordReset';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

const schema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(100, 'Password must be at most 100 characters'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  // If there is no token in the URL there is nothing we can do — redirect immediately.
  useEffect(() => {
    if (!token) {
      navigate('/forgot-password', { replace: true });
    }
  }, [token, navigate]);

  const { mutate: resetPassword, isPending, isSuccess, isError } = useResetPassword();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  const onSubmit = ({ newPassword }: FormValues) => {
    if (!token) return;
    resetPassword({ token, newPassword });
  };

  // Redirect to login after a short delay on success
  useEffect(() => {
    if (!isSuccess) return;
    const timer = setTimeout(() => navigate('/login/customer', { replace: true }), 2500);
    return () => clearTimeout(timer);
  }, [isSuccess, navigate]);

  if (!token) return null;

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-muted/50">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight">Set a new password</CardTitle>
          <CardDescription>Choose a strong password you haven't used before.</CardDescription>
        </CardHeader>

        <CardContent>
          {isSuccess ? (
            /* Success state */
            <div className="rounded-md bg-muted px-4 py-5 text-center space-y-2">
              <p className="text-sm font-medium">Password reset successfully</p>
              <p className="text-sm text-muted-foreground">
                Redirecting you to login…
              </p>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="newPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>New password</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="••••••••"
                          autoComplete="new-password"
                          {...field}
                        />
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
                      <FormLabel>Confirm password</FormLabel>
                      <FormControl>
                        <Input
                          type="password"
                          placeholder="••••••••"
                          autoComplete="new-password"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {isError && (
                  <div className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    This reset link is invalid or has expired.{' '}
                    <Link to="/forgot-password" className="font-medium underline underline-offset-2">
                      Request a new one
                    </Link>
                    .
                  </div>
                )}

                <Button type="submit" className="w-full" disabled={isPending}>
                  {isPending ? 'Saving…' : 'Reset password'}
                </Button>
              </form>
            </Form>
          )}
        </CardContent>

        <CardFooter className="flex justify-center text-sm text-muted-foreground">
          <Link to="/forgot-password" className="hover:underline">
            Back to forgot password
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
}
