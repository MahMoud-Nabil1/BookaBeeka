import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../../redux/hooks';
import { selectIsAuthenticated, selectUserType } from '../../redux/selectors/authSelectors';

interface GuestOnlyProps {
  children: React.ReactNode;
}

export default function GuestOnly({ children }: GuestOnlyProps) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const userType = useAppSelector(selectUserType);

  if (isAuthenticated) {
    if (userType === 'STAFF') {
      return <Navigate to="/staff" replace />;
    }
    return <Navigate to="/portal" replace />;
  }

  return <>{children}</>;
}
