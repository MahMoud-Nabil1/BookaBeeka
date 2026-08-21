export type StaffRole = 'ADMIN' | 'MANAGER' | 'RECEPTIONIST';

export interface CustomerLoginRequest {
  email: string;
  password?: string;
}

export interface StaffLoginRequest {
  email: string;
  password?: string;
}

export interface LoginResponse {
  token: string;
  userType: 'CUSTOMER' | 'STAFF';
}

export interface DecodedStaffToken {
  sub: string;
  user_type: 'STAFF';
  role: StaffRole;
  tenant_id: string;
  branch_id: string;
  exp: number;
}

export interface DecodedCustomerToken {
  sub: string;
  user_type: 'CUSTOMER';
  exp: number;
}

