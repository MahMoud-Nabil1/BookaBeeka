import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAppDispatch } from '../../../redux/hooks';
import { loginSuccess } from '../../../redux/slices/authSlice';
import { staffApi } from '../api/staffApi';
import type { StaffLoginRequest } from '../../../types/auth';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function StaffLoginPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  
  const { mutate: login, isPending, error } = useMutation({
    mutationFn: (req: StaffLoginRequest) => staffApi.staffLogin(req),
    onSuccess: (data) => {
      dispatch(loginSuccess(data.token));
      navigate('/staff'); // StaffRoleRedirect will handle the rest
    },
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
          <CardTitle className="text-2xl font-bold tracking-tight">Staff Portal</CardTitle>
          <CardDescription>Login with your staff credentials</CardDescription>
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
                    <FormLabel>Password</FormLabel>
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
                {isPending ? 'Authenticating...' : 'Login to Dashboard'}
              </Button>
            </form>
          </Form>
        </CardContent>
        <CardFooter className="flex justify-center text-sm">
          <div className="text-muted-foreground">
            Not a staff member?{' '}
            <Link to="/login/customer" className="hover:underline">
              Customer Login
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}

