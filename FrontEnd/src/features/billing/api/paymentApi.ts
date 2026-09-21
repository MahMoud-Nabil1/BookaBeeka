import api from '../../../config/api';
import type {
  CustomerBalanceResponse,
  CustomerTransactionDetail,
  WalletTransactionResponse,
  TopUpRequest,
  Page,
} from '../../../types/payment';
import type { TenantBalanceResponse, TenantPaymentDetail, PaymentRequest, RefundRequest, PaymentResponse } from '../../../types/payment';

export const paymentApi = {
  // ── Customer wallet ───────────────────────────────────────────────────────

  // GET /api/payments/customer/{customerId}/balance
  getBalance: async (customerId: string): Promise<CustomerBalanceResponse> => {
    const response = await api.get(`/api/payments/customer/${customerId}/balance`);
    return response.data;
  },

  // GET /api/payments/history/customer/{customerId}?page=&size=
  getHistory: async (
    customerId: string,
    page = 0,
    size = 10
  ): Promise<Page<CustomerTransactionDetail>> => {
    const response = await api.get(`/api/payments/history/customer/${customerId}`, {
      params: { page, size },
    });
    return response.data;
  },

  // POST /api/payments/wallet/top-up
  topUpWallet: async (req: TopUpRequest): Promise<WalletTransactionResponse> => {
    const response = await api.post('/api/payments/wallet/top-up', req);
    return response.data;
  },

  // POST /api/payments/wallet/checkout
  checkout: async (req: PaymentRequest): Promise<PaymentResponse> => {
    const response = await api.post('/api/payments/wallet/checkout', req);
    return response.data;
  },

  // POST /api/payments/wallet/refund/{bookingId}
  refund: async (bookingId: string, req: RefundRequest): Promise<PaymentResponse> => {
    const response = await api.post(`/api/payments/wallet/refund/${bookingId}`, req);
    return response.data;
  },

  // ── Tenant ────────────────────────────────────────────────────────────────

  // GET /api/payments/tenant/{tenantId}/balance
  getTenantBalance: async (tenantId: string): Promise<TenantBalanceResponse> => {
    const response = await api.get(`/api/payments/tenant/${tenantId}/balance`);
    return response.data;
  },

  // GET /api/payments/history/tenant/{tenantId}?page=&size=
  getTenantHistory: async (
    tenantId: string,
    page = 0,
    size = 10
  ): Promise<Page<TenantPaymentDetail>> => {
    const response = await api.get(`/api/payments/history/tenant/${tenantId}`, {
      params: { page, size },
    });
    return response.data;
  },
};
