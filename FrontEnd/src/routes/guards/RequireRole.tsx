import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../../redux/hooks';
import { selectRole } from '../../redux/selectors/authSelectors';
import type { StaffRole } from '../../types/auth';

interface RequireRoleProps {
  children: React.ReactNode;
  allowedRoles: StaffRole[];
}

export default function RequireRole({ children, allowedRoles }: RequireRoleProps) {
  const role = useAppSelector(selectRole);

  if (!role || !allowedRoles.includes(role)) {
    // If they don't have the required role, bounce them back to the role router
    return <Navigate to="/staff" replace />;
  }

  return <>{children}</>;
}
