import api from '../../../config/api';
import type { CustomerRegisterRequest, Customer } from '../../../types/customer';
import type { CustomerLoginRequest, LoginResponse } from '../../../types/auth';

export const customerApi = {
  register: async (req: CustomerRegisterRequest): Promise<Customer> => {
    const response = await api.post('/customers/register', req);
    return response.data;
  },
  
  customerLogin: async (req: CustomerLoginRequest): Promise<LoginResponse> => {
    const response = await api.post('/auth/customer/login', req);
    return response.data;
  }
};
