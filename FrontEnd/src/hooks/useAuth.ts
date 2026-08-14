import { useAppSelector } from '../redux/hooks'
import {
  selectIsAuthenticated,
  selectUserType,
  selectRole,
  selectTenantId,
  selectBranchId,
  selectUserId,
} from '../redux/selectors/authSelectors'

export function useAuth() {
  return {
    isAuthenticated: useAppSelector(selectIsAuthenticated),
    userType:        useAppSelector(selectUserType),
    role:            useAppSelector(selectRole),
    tenantId:        useAppSelector(selectTenantId),
    branchId:        useAppSelector(selectBranchId),
    userId:          useAppSelector(selectUserId),
  }
}

