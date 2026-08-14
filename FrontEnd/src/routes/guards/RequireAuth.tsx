import { Navigate, useLocation } from 'react-router-dom';
import { useAppSelector } from '../../redux/hooks';
import { selectIsAuthenticated, selectUserType } from '../../redux/selectors/authSelectors';

interface RequireAuthProps {
  children: React.ReactNode;
  allowedUserType?: 'STAFF' | 'CUSTOMER';
}

export default function RequireAuth({ children, allowedUserType }: RequireAuthProps) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const userType = useAppSelector(selectUserType);
  const location = useLocation();

  if (!isAuthenticated) {
    // Redirect to the appropriate login page based on the attempted URL or allowedType
    if (allowedUserType === 'STAFF' || location.pathname.startsWith('/staff')) {
      return <Navigate to="/login/staff" state={{ from: location }} replace />;
    }
    return <Navigate to="/login/customer" state={{ from: location }} replace />;
  }

  if (allowedUserType && userType !== allowedUserType) {
    // Authenticated, but wrong user type (e.g. staff trying to access customer portal)
    if (userType === 'STAFF') {
      return <Navigate to="/staff" replace />;
    }
    return <Navigate to="/portal" replace />;
  }

  return <>{children}</>;
}
