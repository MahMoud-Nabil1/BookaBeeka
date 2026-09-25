import { useMutation } from '@tanstack/react-query';
import { authApi } from '../api/authApi';

/**
 * Submits a forgot-password request. The server always responds 200 OK
 * regardless of whether the email exists (OWASP user-enumeration protection).
 */
export function useForgotPassword() {
  return useMutation({
    mutationFn: (email: string) => authApi.forgotPassword(email),
  });
}

/**
 * Consumes a single-use reset token and sets a new password.
 */
export function useResetPassword() {
  return useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) =>
      authApi.resetPassword(token, newPassword),
  });
}
