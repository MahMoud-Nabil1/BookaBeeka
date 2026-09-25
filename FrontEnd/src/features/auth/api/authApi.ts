import api from '../../../config/api';

export const authApi = {
  /**
   * Requests a password reset email for the given address.
   * Always resolves (server returns 200 regardless of whether the email exists)
   * to prevent user enumeration.
   */
  forgotPassword: async (email: string): Promise<void> => {
    await api.post('/api/auth/password/forgot', { email });
  },

  /**
   * Consumes a single-use reset token and sets a new password.
   * @param token  The UUID token from the reset link query param.
   * @param newPassword  The new password (min 8 chars, validated server-side too).
   */
  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await api.post('/api/auth/password/reset', { token, newPassword });
  },
};
