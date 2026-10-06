// Backend roles: SUPER_ADMIN | OWNER | ADMIN | STAFF
// Frontend only ever sees OWNER, ADMIN, STAFF (SUPER_ADMIN is platform-level only)
export type StaffRole = 'SUPER_ADMIN' | 'OWNER' | 'ADMIN' | 'STAFF';

export interface CustomerLoginRequest {
  email: string;
  password: string;
}

export interface OwnerLoginRequest {
  email: string;
  password: string;
}

export interface SuperAdminLoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userType: 'CUSTOMER' | 'STAFF';
}

export interface DecodedStaffToken {
  sub: string;          // staff UUID
  user_type?: 'STAFF';
  role: StaffRole;
  tenant_id: string;    // UUID string, null for SUPER_ADMIN
  branch_id?: string;   // UUID string, null for SUPER_ADMIN
  iat: number;
  exp: number;
}

export interface OwnerRegisterRequest {
  hotelName: string;
  subdomain: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
  currency?: string;
  timezone?: string;
}

export interface OwnerRegisterResponse {
  adminId: string;
  fullName: string;
  email: string;
  role: string;
  message: string;
}

export interface DecodedCustomerToken {
  sub: string;          // customer UUID
  user_type?: 'CUSTOMER';
  role?: string;
  iat: number;
  exp: number;
}
