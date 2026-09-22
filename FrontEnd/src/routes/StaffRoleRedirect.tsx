import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../redux/hooks';
import { selectRole } from '../redux/selectors/authSelectors';

export default function StaffRoleRedirect() {
  const role = useAppSelector(selectRole);

  switch (role) {
    case 'SUPER_ADMIN':
    case 'OWNER':
    case 'ADMIN':
      return <Navigate to="/staff/admin/overview" replace />;
    case 'STAFF':
      return <Navigate to="/staff/receptionist/bookings" replace />;
    default:
      // Unknown or missing role — re-auth
      return <Navigate to="/login/staff" replace />;
  }
}
