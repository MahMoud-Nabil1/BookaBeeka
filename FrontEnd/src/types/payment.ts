export interface CustomerBalanceResponse {
  walletId: string;
  customerId: string;
  balance: number;
  currency: string;
  updatedAt: string;
}

export interface CustomerTransactionDetail {
  transactionId: string;
  transactionType: 'DEPOSIT' | 'PAYMENT' | 'REFUND';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  currency: string;
  bookingId: string | null;
  description: string;
  createdAt: string;
}

export interface WalletTransactionResponse {
  id: string;
  walletId: string;
  bookingId: string | null;
  amount: number;
  transactionType: string;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface TopUpRequest {
  customerId: string;
  amount: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
