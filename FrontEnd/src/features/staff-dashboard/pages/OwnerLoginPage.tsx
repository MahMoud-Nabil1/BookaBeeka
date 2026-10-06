import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAppDispatch } from '../../../redux/hooks';
import { loginSuccess } from '../../../redux/slices/authSlice';
import { ownerApi } from '../api/ownerApi';
import type { OwnerLoginRequest } from '../../../types/auth';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function OwnerLoginPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  
  const { mutate: login, isPending, error } = useMutation({
    mutationFn: (req: OwnerLoginRequest) => ownerApi.ownerLogin(req),
    onSuccess: (data) => {
      console.log('Login successful, response:', data);
      dispatch(loginSuccess(data.token));
      navigate('/staff'); // StaffRoleRedirect will handle the rest
    },
    onError: (err: any) => {
      console.error('Login failed:', err);
      console.error('Error response:', err.response?.data);
    }
  });

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = (values: LoginFormValues) => {
    login(values);
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-muted/50">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold tracking-tight">Owner Portal</CardTitle>
          <CardDescription>Login as Hotel Owner or Branch Admin</CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input placeholder="admin@example.com" type="email" {...field} />
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
                    <div className="flex items-center justify-between">
                      <FormLabel>Password</FormLabel>
                      <Link
                        to="/forgot-password"
                        className="text-xs text-muted-foreground hover:underline"
                        tabIndex={-1}
                      >
                        Forgot password?
                      </Link>
                    </div>
                    <FormControl>
                      <Input type="password" placeholder="••••••••" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              {error && (
                <div className="text-sm font-medium text-destructive">
                  Invalid email or password
                </div>
              )}
              <Button type="submit" className="w-full" disabled={isPending}>
                {isPending ? 'Signing in...' : 'Sign In'}
              </Button>
            </form>
          </Form>
        </CardContent>
        <CardFooter className="flex flex-col space-y-2 text-sm text-center border-t border-border pt-4">
          <div className="text-muted-foreground">
            Don't have an account?{' '}
            <Link to="/register/owner" className="hover:underline text-primary font-medium">
              Create one
            </Link>
          </div>
          <div className="text-muted-foreground">
            Platform administrator?{' '}
            <Link to="/login/superadmin" className="hover:underline text-primary">
              SuperAdmin Portal
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

