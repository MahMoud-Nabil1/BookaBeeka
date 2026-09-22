import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';

// Guards
import GuestOnly from './guards/GuestOnly';
import RequireAuth from './guards/RequireAuth';
import RequireRole from './guards/RequireRole';
import StaffRoleRedirect from './StaffRoleRedirect';

// Public / Auth pages
import CustomerLoginPage from '../features/customer-portal/pages/CustomerLoginPage';
import CustomerRegisterPage from '../features/customer-portal/pages/CustomerRegisterPage';
import CustomerPortalLayout from '../features/customer-portal/pages/CustomerPortalLayout';
import StaffLoginPage from '../features/staff-dashboard/pages/StaffLoginPage';
import LandingPage from '../pages/LandingPage';

// Customer portal pages
import CustomerBookingsPage from '../features/bookings/pages/CustomerBookingsPage';
import BookingDetailPage from '../features/bookings/pages/BookingDetailPage';
import CustomerWalletPage from '../features/billing/pages/CustomerWalletPage';
import CatalogPage from '../features/catalog/pages/CatalogPage';
import RoomDetailPage from '../features/catalog/pages/RoomDetailPage';
import ProfilePage from '../features/profile/pages/ProfilePage';

// Admin dashboard (SUPER_ADMIN, OWNER, ADMIN)
import AdminDashboardLayout from '../features/staff-dashboard/admin/pages/AdminDashboardLayout';
import AdminOverviewPage from '../features/staff-dashboard/admin/pages/AdminOverviewPage';
import AdminBookingsPage from '../features/staff-dashboard/admin/pages/AdminBookingsPage';
import AdminPaymentsPage from '../features/staff-dashboard/admin/pages/AdminPaymentsPage';
import AdminSchedulePage from '../features/staff-dashboard/admin/pages/AdminSchedulePage';

// Receptionist dashboard (STAFF role)
import ReceptionistDashboardLayout from '../features/staff-dashboard/receptionist/pages/ReceptionistDashboardLayout';
import ReceptionistBookingsPage from '../features/staff-dashboard/receptionist/pages/ReceptionistBookingsPage';

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* ═══════════════════════════════════════════════════════════
            PUBLIC ROUTES (No authentication required)
        ═══════════════════════════════════════════════════════════ */}
        
        {/* Landing page */}
        <Route path="/" element={<LandingPage />} />
        
        {/* Auth pages - only accessible when NOT logged in */}
        <Route element={<GuestOnly><Outlet /></GuestOnly>}>
          <Route path="/login/customer" element={<CustomerLoginPage />} />
          <Route path="/login/staff" element={<StaffLoginPage />} />
          <Route path="/register" element={<CustomerRegisterPage />} />
        </Route>

        {/* ═══════════════════════════════════════════════════════════
            CUSTOMER PORTAL (Requires CUSTOMER authentication)
        ═══════════════════════════════════════════════════════════ */}
        
        <Route element={<RequireAuth allowedUserType="CUSTOMER"><Outlet /></RequireAuth>}>
          <Route path="/portal" element={<CustomerPortalLayout />}>
            {/* Browse rooms (catalog) */}
            <Route index element={<CatalogPage />} />
            <Route path="rooms" element={<CatalogPage />} />
            <Route path="rooms/:roomId" element={<RoomDetailPage />} />
            
            {/* Bookings */}
            <Route path="bookings" element={<CustomerBookingsPage />} />
            <Route path="bookings/:bookingId" element={<BookingDetailPage />} />
            
            {/* Wallet */}
            <Route path="wallet" element={<CustomerWalletPage />} />
            
            {/* Profile */}
            <Route path="profile" element={<ProfilePage />} />
            
            {/* Legacy route redirects for backward compatibility */}
            <Route path="catalog" element={<Navigate to="/portal/rooms" replace />} />
            <Route path="catalog/:roomId" element={<RoomDetailPage />} />
          </Route>
        </Route>

        {/* ═══════════════════════════════════════════════════════════
            STAFF PORTAL (Requires STAFF authentication)
        ═══════════════════════════════════════════════════════════ */}
        
        <Route path="/staff" element={<RequireAuth allowedUserType="STAFF"><Outlet /></RequireAuth>}>
          {/* Role-based redirect */}
          <Route index element={<StaffRoleRedirect />} />
          
          {/* Admin dashboard (SUPER_ADMIN, OWNER, ADMIN roles) */}
          <Route path="admin" element={<RequireRole allowedRoles={['SUPER_ADMIN', 'OWNER', 'ADMIN']}><Outlet /></RequireRole>}>
            <Route element={<AdminDashboardLayout />}>
              <Route index element={<Navigate to="/staff/admin/overview" replace />} />
              <Route path="overview" element={<AdminOverviewPage />} />
              <Route path="bookings" element={<AdminBookingsPage />} />
              <Route path="payments" element={<AdminPaymentsPage />} />
              <Route path="schedule" element={<AdminSchedulePage />} />
            </Route>
          </Route>
          
          {/* Receptionist dashboard (STAFF role) */}
          <Route path="receptionist" element={<RequireRole allowedRoles={['STAFF']}><Outlet /></RequireRole>}>
            <Route element={<ReceptionistDashboardLayout />}>
              <Route index element={<Navigate to="/staff/receptionist/bookings" replace />} />
              <Route path="bookings" element={<ReceptionistBookingsPage />} />
            </Route>
          </Route>
        </Route>

        {/* ═══════════════════════════════════════════════════════════
            FALLBACK - Redirect any unknown routes to home
        ═══════════════════════════════════════════════════════════ */}
        
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
