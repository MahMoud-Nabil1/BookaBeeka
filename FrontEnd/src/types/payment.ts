// ── Customer ──────────────────────────────────────────────────────────────────

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
  transactionType: 'DEPOSIT' | 'PAYMENT' | 'REFUND';
  balanceAfter: number;
  description: string;
  createdAt: string;
}

// ── Tenant ────────────────────────────────────────────────────────────────────

export interface TenantBalanceResponse {
  tenantWalletId: string;
  tenantId: string;
  balance: number;
  currency: string;
  updatedAt: string;
}

export interface TenantPaymentDetail {
  paymentId: string;
  bookingId: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
  paymentMethod: 'WALLET';
  amount: number;
  currency: string;
  createdAt: string;
  updatedAt: string;
}

// ── Request bodies ────────────────────────────────────────────────────────────

export interface TopUpRequest {
  customerId: string;
  amount: number;
}

export interface PaymentRequest {
  bookingId: string;
  customerId: string;
  tenantId: string;
  paymentAmount: number;
  idempotencyKey?: string;
  expectedCurrency?: string;
}

export interface RefundRequest {
  bookingId: string;
  customerId: string;
  tenantId: string;
}

export interface PaymentResponse {
  id: string;
  bookingId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
  paymentMethod: 'WALLET';
  createdAt: string;
}

// ── Pagination ────────────────────────────────────────────────────────────────

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
