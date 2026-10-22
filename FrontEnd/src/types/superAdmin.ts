// ── Super Admin Platform Types ────────────────────────────────────────────────
// Matches DTOs from /api/admin/super/* endpoints (SUPER_ADMIN role only)

export interface PlatformStats {
  // Tenants
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  bannedTenants: number;

  // Customers
  totalCustomers: number;
  bannedCustomers: number;

  // Hotel Users
  totalHotelUsers: number;

  // Bookings
  totalBookings: number;
  confirmedBookings: number;
  completedBookings: number;
  cancelledBookings: number;
  stuckBookings: number;

  // Payments
  failedPaymentsCount: number;

  // Financials
  platformRevenue: number;
  moneyInCirculation: number;
}

export interface TenantSummary {
  id: string;
  name: string;
  subdomain: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'BANNED' | string;
  currency: string;
  timezone: string;
  ownersCount: number;
  adminsCount: number;
  suspendedAt?: string | null;
  suspendedBy?: string | null;
  suspendedReason?: string | null;
  createdAt: string;
}

export interface TenantDetail {
  id: string;
  name: string;
  subdomain: string;
  status: string;
  currency: string;
  timezone: string;
  revenueBalance: number;
  totalBookings: number;
  confirmedBookings: number;
  completedBookings: number;
  cancelledBookings: number;
  ownersCount?: number;
  adminsCount?: number;
  suspendedAt?: string | null;
  suspendedBy?: string | null;
  suspendedReason?: string | null;
  createdAt: string;
}

export interface CustomerSummary {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  isActive: boolean;
  banned: boolean;
  bannedAt?: string | null;
  bannedBy?: string | null;
  banReason?: string | null;
  walletBalance?: number | null;
  createdAt: string;
}

export interface PlatformTransaction {
  transactionId: string;
  transactionType: 'DEPOSIT' | 'PAYMENT' | 'REFUND' | string;
  amount: number;
  balanceAfter: number;
  fromCustomerId: string;
  fromCustomerEmail: string;
  fromCustomerName: string;
  toTenantId: string | null;
  bookingId: string | null;
  paymentId: string | null;
  description: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;    // current page (0-indexed)
  size: number;
}
