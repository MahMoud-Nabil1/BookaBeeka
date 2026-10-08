import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useCustomerRegister } from '../hooks/useCustomerAuth';
import { useMutation } from '@tanstack/react-query';
import { customerApi } from '../api/customerApi';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

// ── Step 1 schema ──────────────────────────────────────────────────────────────
const registerSchema = z.object({
  firstName: z.string().min(2, 'First name is required'),
  lastName: z.string().min(2, 'Last name is required'),
  email: z.string().email('Invalid email address'),
  phone: z.string().optional(),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

// ── Step 2 schema ──────────────────────────────────────────────────────────────
const otpSchema = z.object({
  otpCode: z
    .string()
    .length(6, 'OTP must be exactly 6 digits')
    .regex(/^\d{6}$/, 'OTP must contain only digits'),
});

type RegisterFormValues = z.infer<typeof registerSchema>;
type OtpFormValues = z.infer<typeof otpSchema>;

export default function CustomerRegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'register' | 'verify'>('register');
  const [registeredEmail, setRegisteredEmail] = useState('');

  // ── Step 1: Registration ───────────────────────────────────────────────────
  const { mutate: register, isPending: isRegistering, error: registerError } = useCustomerRegister();

  const registerForm = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { firstName: '', lastName: '', email: '', phone: '', password: '' },
  });

  const onRegister = (values: RegisterFormValues) => {
    register(values, {
      onSuccess: () => {
        // OTP was automatically dispatched by the backend after commit.
        // Move the user to the OTP verification step.
        setRegisteredEmail(values.email);
        setStep('verify');
      },
    });
  };

  // ── Step 2: OTP verification ───────────────────────────────────────────────
  const { mutate: verifyOtp, isPending: isVerifying, error: otpError } = useMutation({
    mutationFn: (code: string) => customerApi.verifyOtp(registeredEmail, code),
    onSuccess: () => {
      // Verification complete → send to login
      navigate('/login/customer');
    },
  });

  const { mutate: resendOtp, isPending: isResending } = useMutation({
    mutationFn: () => customerApi.requestOtp(registeredEmail),
  });

  const otpForm = useForm<OtpFormValues>({
    resolver: zodResolver(otpSchema),
    defaultValues: { otpCode: '' },
  });

  const onVerify = (values: OtpFormValues) => {
    verifyOtp(values.otpCode);
  };

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-muted/50">
      <Card className="w-full max-w-md">

        {/* ── Step 1: Registration form ──────────────────────────────────── */}
        {step === 'register' && (
          <>
            <CardHeader className="space-y-1 text-center">
              <CardTitle className="text-2xl font-bold tracking-tight">Create an Account</CardTitle>
              <CardDescription>Join BookaBeeka to manage your bookings</CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...registerForm}>
                <form onSubmit={registerForm.handleSubmit(onRegister)} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={registerForm.control}
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
                      control={registerForm.control}
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
                    control={registerForm.control}
                    name="email"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Email</FormLabel>
                        <FormControl>
                          <Input placeholder="you@example.com" type="email" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={registerForm.control}
                    name="phone"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Phone (optional)</FormLabel>
                        <FormControl>
                          <Input placeholder="+1234567890" type="tel" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={registerForm.control}
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
                  {registerError && (
                    <div className="text-sm font-medium text-destructive">
                      {(registerError as any)?.response?.data?.message ?? 'Registration failed. Please try again.'}
                    </div>
                  )}
                  <Button type="submit" className="w-full" disabled={isRegistering}>
                    {isRegistering ? 'Creating account…' : 'Create Account'}
                  </Button>
                </form>
              </Form>
            </CardContent>
            <CardFooter className="flex justify-center text-sm">
              <div className="text-muted-foreground">
                Already have an account?{' '}
                <Link to="/login/customer" className="font-medium text-primary hover:underline">
                  Sign in
                </Link>
              </div>
            </CardFooter>
          </>
        )}

        {/* ── Step 2: OTP verification ───────────────────────────────────── */}
        {step === 'verify' && (
          <>
            <CardHeader className="space-y-1 text-center">
              <CardTitle className="text-2xl font-bold tracking-tight">Verify your email</CardTitle>
              <CardDescription>
                We sent a 6-digit code to <span className="font-medium text-foreground">{registeredEmail}</span>.
                Enter it below to activate your account. The code expires in 10 minutes.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...otpForm}>
                <form onSubmit={otpForm.handleSubmit(onVerify)} className="space-y-4">
                  <FormField
                    control={otpForm.control}
                    name="otpCode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Verification Code</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="123456"
                            maxLength={6}
                            inputMode="numeric"
                            autoComplete="one-time-code"
                            className="text-center tracking-[0.5em] text-lg font-mono"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  {otpError && (
                    <div className="text-sm font-medium text-destructive">
                      {(otpError as any)?.response?.data?.message ?? 'Invalid or expired code. Please try again.'}
                    </div>
                  )}
                  <Button type="submit" className="w-full" disabled={isVerifying}>
                    {isVerifying ? 'Verifying…' : 'Verify & Continue'}
                  </Button>
                </form>
              </Form>
            </CardContent>
            <CardFooter className="flex flex-col items-center gap-2 text-sm text-muted-foreground">
              <p>Didn't receive the code? Check your spam folder first.</p>
              <Button
                variant="ghost"
                size="sm"
                disabled={isResending}
                onClick={() => resendOtp()}
              >
                {isResending ? 'Sending…' : 'Resend code'}
              </Button>
            </CardFooter>
          </>
        )}

      </Card>
    </div>
  );
}
