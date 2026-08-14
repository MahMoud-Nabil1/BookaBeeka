import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import { jwtDecode } from 'jwt-decode';
import type { DecodedStaffToken, DecodedCustomerToken, StaffRole } from '../../types/auth';

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
        // We peek at the token to determine user_type
        const baseDecoded = jwtDecode<{ user_type: 'STAFF' | 'CUSTOMER' }>(token);
        
        state.token = token;
        state.userType = baseDecoded.user_type;

        if (baseDecoded.user_type === 'STAFF') {
          const decoded = jwtDecode<DecodedStaffToken>(token);
          state.userId = decoded.sub;
          state.role = decoded.role;
          state.tenantId = decoded.tenant_id;
          state.branchId = decoded.branch_id;
        } else {
          const decoded = jwtDecode<DecodedCustomerToken>(token);
          state.userId = decoded.sub;
          state.role = null;
          state.tenantId = null;
          state.branchId = null;
        }

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
          // Check expiration
          const decoded = jwtDecode<{ exp: number; user_type: 'STAFF' | 'CUSTOMER' }>(token);
          if (decoded.exp * 1000 < Date.now()) {
            localStorage.removeItem('auth_token');
            return;
          }
          
          state.token = token;
          state.userType = decoded.user_type;
          
          if (decoded.user_type === 'STAFF') {
            const staffDecoded = jwtDecode<DecodedStaffToken>(token);
            state.userId = staffDecoded.sub;
            state.role = staffDecoded.role;
            state.tenantId = staffDecoded.tenant_id;
            state.branchId = staffDecoded.branch_id;
          } else {
            const customerDecoded = jwtDecode<DecodedCustomerToken>(token);
            state.userId = customerDecoded.sub;
            state.role = null;
            state.tenantId = null;
            state.branchId = null;
          }
        } catch (err) {
          localStorage.removeItem('auth_token');
        }
      }
    },
  },
});

export const { loginSuccess, logout, initFromStorage } = authSlice.actions;
export default authSlice.reducer;

