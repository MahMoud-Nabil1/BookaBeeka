import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import { jwtDecode } from 'jwt-decode';
import type { StaffRole } from '../../types/auth';

interface TokenClaims {
  sub: string;
  role?: string;
  user_type?: 'STAFF' | 'CUSTOMER';
  tenant_id?: string | null;
  branch_id?: string | null;
  exp?: number;
}

function parseToken(token: string) {
  const decoded = jwtDecode<TokenClaims>(token);
  const role = decoded.role;
  const isCustomer = role === 'CUSTOMER' || decoded.user_type === 'CUSTOMER';
  const isStaff = !isCustomer && (role === 'STAFF' || role === 'ADMIN' || role === 'OWNER' || role === 'SUPER_ADMIN' || decoded.user_type === 'STAFF');
  const userType: 'STAFF' | 'CUSTOMER' | null = isStaff ? 'STAFF' : (isCustomer ? 'CUSTOMER' : null);

  return {
    userId: decoded.sub || null,
    userType,
    role: isStaff ? (role as StaffRole) : null,
    tenantId: decoded.tenant_id ?? null,
    branchId: decoded.branch_id ?? null,
    exp: decoded.exp,
  };
}

interface AuthState {
  token: string | null;
  userType: 'STAFF' | 'CUSTOMER' | null;
  userId: string | null;
  role: StaffRole | null;
  tenantId: string | null;
  branchId: string | null;
}

const initialState: AuthState = {
  token: null,
  userType: null,
  userId: null,
  role: null,
  tenantId: null,
  branchId: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginSuccess(state, action: PayloadAction<string>) {
      const token = action.payload;
      try {
        const parsed = parseToken(token);
        
        state.token = token;
        state.userType = parsed.userType;
        state.userId = parsed.userId;
        state.role = parsed.role;
        state.tenantId = parsed.tenantId;
        state.branchId = parsed.branchId;

        // Persist to localStorage
        localStorage.setItem('auth_token', token);
      } catch (err) {
        console.error('Invalid token received', err);
      }
    },
    logout(state) {
      state.token = null;
      state.userType = null;
      state.userId = null;
      state.role = null;
      state.tenantId = null;
      state.branchId = null;
      localStorage.removeItem('auth_token');
    },
    initFromStorage(state) {
      const token = localStorage.getItem('auth_token');
      if (token) {
        try {
          const parsed = parseToken(token);
          // Check expiration
          if (parsed.exp && parsed.exp * 1000 < Date.now()) {
            localStorage.removeItem('auth_token');
            return;
          }
          
          state.token = token;
          state.userType = parsed.userType;
          state.userId = parsed.userId;
          state.role = parsed.role;
          state.tenantId = parsed.tenantId;
          state.branchId = parsed.branchId;
        } catch (err) {
          localStorage.removeItem('auth_token');
        }
      }
    },
  },
});

export const { loginSuccess, logout, initFromStorage } = authSlice.actions;
export default authSlice.reducer;

