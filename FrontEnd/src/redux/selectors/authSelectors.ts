import type { RootState } from '../index';

export const selectIsAuthenticated = (state: RootState) => !!state.auth.token;
export const selectUserType = (state: RootState) => state.auth.userType;
export const selectRole = (state: RootState) => state.auth.role;
export const selectTenantId = (state: RootState) => state.auth.tenantId;
export const selectBranchId = (state: RootState) => state.auth.branchId;
export const selectUserId = (state: RootState) => state.auth.userId;

