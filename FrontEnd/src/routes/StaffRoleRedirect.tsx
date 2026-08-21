import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../redux/hooks';
import { selectRole } from '../redux/selectors/authSelectors';

export default function StaffRoleRedirect() {
  const role = useAppSelector(selectRole);

  switch (role) {
    case 'ADMIN':
      return <Navigate to="/staff/admin/overview" replace />;
    case 'MANAGER':
      return <Navigate to="/staff/manager/bookings" replace />;
    case 'RECEPTIONIST':
      return <Navigate to="/staff/receptionist/bookings" replace />;
    default:
      // Fallback if role is unknown or missing, send to login to re-auth
      return <Navigate to="/login/staff" replace />;
  }
}
