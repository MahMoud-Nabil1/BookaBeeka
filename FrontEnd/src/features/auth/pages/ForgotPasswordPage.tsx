import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Link } from 'react-router-dom';
import { useForgotPassword } from '../hooks/usePasswordReset';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
});

type FormValues = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const { mutate: requestReset, isPending, isSuccess, isError } = useForgotPassword();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '' },
  });

  const onSubmit = ({ email }: FormValues) => {
    requestReset(email);
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-muted/50">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight">Forgot your password?</CardTitle>
          <CardDescription>
            Enter your email and we'll send you a reset link.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {isSuccess ? (
            /* Success state — neutral message, no user-enumeration leak */
            <div className="rounded-md bg-muted px-4 py-5 text-center space-y-2">
              <p className="text-sm font-medium">Check your inbox</p>
              <p className="text-sm text-muted-foreground">
                If an account exists for that email address, a password reset link has been sent.
                The link expires in 15 minutes.
              </p>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Email address</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder="you@example.com"
                          autoComplete="email"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {isError && (
                  <p className="text-sm font-medium text-destructive">
                    Something went wrong. Please try again.
                  </p>
                )}

                <Button type="submit" className="w-full" disabled={isPending}>
                  {isPending ? 'Sending...' : 'Send reset link'}
                </Button>
              </form>
            </Form>
          )}
        </CardContent>

        <CardFooter className="flex justify-center gap-4 text-sm text-muted-foreground">
          <Link to="/login/customer" className="hover:underline">
            Customer login
          </Link>
          <span>·</span>
          <Link to="/login/owner" className="hover:underline">
            Owner login
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
}
