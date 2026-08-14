import { useMutation } from '@tanstack/react-query';
import { useAppDispatch } from '../../../redux/hooks';
import { loginSuccess } from '../../../redux/slices/authSlice';
import { customerApi } from '../api/customerApi';
import type { CustomerLoginRequest } from '../../../types/auth';

export function useCustomerLogin() {
  const dispatch = useAppDispatch();
  return useMutation({
    mutationFn: (req: CustomerLoginRequest) => customerApi.customerLogin(req),
    onSuccess: (data) => {
      dispatch(loginSuccess(data.token));
    },
  });
}

export function useCustomerRegister() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return useMutation({
    mutationFn: (req: any) => customerApi.register(req),
  });
}

