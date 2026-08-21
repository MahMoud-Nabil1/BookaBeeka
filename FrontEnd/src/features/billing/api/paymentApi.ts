import api from '../../../config/api';
import type { 
  CustomerBalanceResponse, 
  CustomerTransactionDetail, 
  Page, 
  TopUpRequest, 
  WalletTransactionResponse 
} from '../../../types/payment';

export const paymentApi = {
  getBalance: async (customerId: string): Promise<CustomerBalanceResponse> => {
    const response = await api.get('/api/payments/balance', {
      params: { customerId }
    });
    return response.data;
  },

  getHistory: async (customerId: string, page: number = 0, size: number = 10): Promise<Page<CustomerTransactionDetail>> => {
    const response = await api.get('/api/payments/history', {
      params: { customerId, page, size }
    });
    return response.data;
  },

  topUpWallet: async (req: TopUpRequest): Promise<WalletTransactionResponse> => {
    const response = await api.post('/api/payments/wallet/top-up', req);
    return response.data;
  }
};
