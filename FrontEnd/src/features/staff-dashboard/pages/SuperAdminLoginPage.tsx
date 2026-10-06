import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAppDispatch } from '../../../redux/hooks';
import { loginSuccess } from '../../../redux/slices/authSlice';
import { superAdminApi } from '../api/superAdminApi';
import type { SuperAdminLoginRequest } from '../../../types/auth';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Shield } from 'lucide-react';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function SuperAdminLoginPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  
  const { mutate: login, isPending, error } = useMutation({
    mutationFn: (req: SuperAdminLoginRequest) => superAdminApi.superAdminLogin(req),
    onSuccess: (data) => {
      console.log('SuperAdmin login successful, response:', data);
      dispatch(loginSuccess(data.token));
      navigate('/staff'); // StaffRoleRedirect will handle the rest
    },
    onError: (err: any) => {
      console.error('SuperAdmin login failed:', err);
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
    <div className="flex min-h-screen items-center justify-center p-4 bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <Card className="w-full max-w-md border-purple-500/20 shadow-2xl">
        <CardHeader className="space-y-3 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-purple-600/10 border-2 border-purple-500/30">
            <Shield className="h-7 w-7 text-purple-500" />
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight">SuperAdmin Portal</CardTitle>
          <CardDescription className="text-base">
            Platform-level administrative access
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="mb-6 p-3 bg-purple-500/5 border border-purple-500/20 rounded-lg">
            <p className="text-xs text-muted-foreground text-center">
              <Shield className="inline h-3 w-3 mr-1" />
              This portal is restricted to platform administrators only
            </p>
          </div>
          
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Administrator Email</FormLabel>
                    <FormControl>
                      <Input placeholder="superadmin@bookabeeka.com" type="email" {...field} />
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
                  Invalid credentials or unauthorized access
                </div>
              )}
              <Button type="submit" className="w-full bg-purple-600 hover:bg-purple-700" disabled={isPending}>
                {isPending ? 'Authenticating...' : 'Access Platform'}
              </Button>
            </form>
          </Form>
        </CardContent>
        <CardFooter className="flex flex-col space-y-2 text-sm text-center border-t border-border pt-4">
          <div className="text-muted-foreground">
            Hotel owner?{' '}
            <Link to="/login/owner" className="hover:underline text-primary">
              Owner Portal
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
