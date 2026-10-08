import api from '../../../config/api';
import type { CustomerRegisterRequest, Customer } from '../../../types/customer';
import type { CustomerLoginRequest, LoginResponse } from '../../../types/auth';

/** Platform-level tenant ID used for cross-tenant customer OTP verification. */
const PLATFORM_TENANT_ID = '00000000-0000-0000-0000-000000000000';

export const customerApi = {
  register: async (req: CustomerRegisterRequest): Promise<Customer> => {
    const response = await api.post('/api/customers/register', req);
    return response.data;
  },

  customerLogin: async (req: CustomerLoginRequest): Promise<LoginResponse> => {
    const response = await api.post('/api/auth/customer/login', req);
    return response.data;
  },

  /**
   * Requests a new OTP for the given email. Backend dispatches an email asynchronously.
   * Always returns 200 (OWASP anti-enumeration) so we don't surface whether the email exists.
   */
  requestOtp: async (email: string): Promise<void> => {
    await api.post('/api/auth/otp/request', { email, tenantId: PLATFORM_TENANT_ID });
  },

  /**
   * Verifies a 6-digit OTP code submitted by the user.
   * Sends the platform tenant ID so the backend resolves the correct in-memory key.
   */
  verifyOtp: async (email: string, otpCode: string): Promise<void> => {
    await api.post('/api/auth/otp/verify', {
      email,
      otpCode,
      tenantId: PLATFORM_TENANT_ID,
    });
  },
};
