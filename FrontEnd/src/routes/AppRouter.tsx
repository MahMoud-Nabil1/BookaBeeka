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
import OwnerLoginPage from '../features/staff-dashboard/pages/OwnerLoginPage';
import OwnerRegisterPage from '../features/staff-dashboard/pages/OwnerRegisterPage';
import SuperAdminLoginPage from '../features/staff-dashboard/pages/SuperAdminLoginPage';
import LandingPage from '../pages/LandingPage';
import ForgotPasswordPage from '../features/auth/pages/ForgotPasswordPage';
import ResetPasswordPage from '../features/auth/pages/ResetPasswordPage';

// Customer portal pages
import CustomerBookingsPage from '../features/bookings/pages/CustomerBookingsPage';
import BookingDetailPage from '../features/bookings/pages/BookingDetailPage';
import CustomerWalletPage from '../features/billing/pages/CustomerWalletPage';
import CatalogPage from '../features/catalog/pages/CatalogPage';
import RoomDetailPage from '../features/catalog/pages/RoomDetailPage';
import ProfilePage from '../features/profile/pages/ProfilePage';

// Super Admin dashboard (SUPER_ADMIN role only)
import SuperAdminDashboardLayout from '../features/staff-dashboard/superadmin/pages/SuperAdminDashboardLayout';
import SuperAdminOverviewPage from '../features/staff-dashboard/superadmin/pages/SuperAdminOverviewPage';
import SuperAdminHotelsPage from '../features/staff-dashboard/superadmin/pages/SuperAdminHotelsPage';
import SuperAdminCustomersPage from '../features/staff-dashboard/superadmin/pages/SuperAdminCustomersPage';
import SuperAdminRoomsPage from '../features/staff-dashboard/superadmin/pages/SuperAdminRoomsPage';
import SuperAdminBookingsPage from '../features/staff-dashboard/superadmin/pages/SuperAdminBookingsPage';
import SuperAdminPaymentsPage from '../features/staff-dashboard/superadmin/pages/SuperAdminPaymentsPage';

// Owner dashboard (OWNER role only)
import OwnerDashboardLayout from '../features/staff-dashboard/owner/pages/OwnerDashboardLayout';
import OwnerOverviewPage from '../features/staff-dashboard/owner/pages/OwnerOverviewPage';
import OwnerManageAdminsPage from '../features/staff-dashboard/owner/pages/OwnerManageAdminsPage';

// Admin dashboard (ADMIN role only)
import AdminDashboardLayout from '../features/staff-dashboard/admin/pages/AdminDashboardLayout';
import AdminOverviewPage from '../features/staff-dashboard/admin/pages/AdminOverviewPage';
import AdminBookingsPage from '../features/staff-dashboard/admin/pages/AdminBookingsPage';
import AdminPaymentsPage from '../features/staff-dashboard/admin/pages/AdminPaymentsPage';
import AdminSchedulePage from '../features/staff-dashboard/admin/pages/AdminSchedulePage';
import AdminRoomBlocksPage from '../features/staff-dashboard/admin/pages/AdminRoomBlocksPage';
import { AdminAmenitiesPage } from '../features/staff-dashboard/admin/pages/AdminAmenitiesPage';
import { AdminRoomsPage } from '../features/staff-dashboard/admin/pages/AdminRoomsPage';

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
          <Route path="/login/owner" element={<OwnerLoginPage />} />
          <Route path="/register/owner" element={<OwnerRegisterPage />} />
          <Route path="/login/superadmin" element={<SuperAdminLoginPage />} />
          <Route path="/register" element={<CustomerRegisterPage />} />
        </Route>

        {/* Password recovery routes accessible regardless of auth state */}
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

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

            {/* Fallback for unknown /portal routes */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Route>

        {/* ═══════════════════════════════════════════════════════════
            STAFF PORTAL (Requires STAFF authentication)
        ═══════════════════════════════════════════════════════════ */}
        
        <Route path="/staff" element={<RequireAuth allowedUserType="STAFF"><Outlet /></RequireAuth>}>
          {/* Role-based redirect */}
          <Route index element={<StaffRoleRedirect />} />

          {/* ── Super Admin dashboard (SUPER_ADMIN role only) ─────────── */}
          <Route path="superadmin" element={<RequireRole allowedRoles={['SUPER_ADMIN']}><Outlet /></RequireRole>}>
            <Route element={<SuperAdminDashboardLayout />}>
              <Route index element={<Navigate to="/staff/superadmin/overview" replace />} />
              <Route path="overview"  element={<SuperAdminOverviewPage />} />
              <Route path="hotels"    element={<SuperAdminHotelsPage />} />
              <Route path="customers" element={<SuperAdminCustomersPage />} />
              <Route path="rooms"     element={<SuperAdminRoomsPage />} />
              <Route path="bookings"  element={<SuperAdminBookingsPage />} />
              <Route path="payments"  element={<SuperAdminPaymentsPage />} />
              <Route path="*" element={<Navigate to="/staff/superadmin/overview" replace />} />
            </Route>
          </Route>
          
          {/* ── Owner dashboard (OWNER role only) ────────────────────── */}
          <Route path="owner" element={<RequireRole allowedRoles={['OWNER']}><Outlet /></RequireRole>}>
            <Route element={<OwnerDashboardLayout />}>
              <Route index element={<Navigate to="/staff/owner/overview" replace />} />
              <Route path="overview"  element={<OwnerOverviewPage />} />
              <Route path="bookings"  element={<AdminBookingsPage />} />
              <Route path="payments"  element={<AdminPaymentsPage />} />
              <Route path="rooms"     element={<AdminRoomsPage />} />
              <Route path="amenities" element={<AdminAmenitiesPage />} />
              <Route path="admins"    element={<OwnerManageAdminsPage />} />
              <Route path="*" element={<Navigate to="/staff/owner/overview" replace />} />
            </Route>
          </Route>

          {/* ── Admin dashboard (ADMIN role only) ─────────────────────── */}
          <Route path="admin" element={<RequireRole allowedRoles={['ADMIN']}><Outlet /></RequireRole>}>
            <Route element={<AdminDashboardLayout />}>
              <Route index element={<Navigate to="/staff/admin/overview" replace />} />
              <Route path="overview" element={<AdminOverviewPage />} />
              <Route path="bookings" element={<AdminBookingsPage />} />
              <Route path="payments" element={<AdminPaymentsPage />} />
              <Route path="schedule" element={<AdminSchedulePage />} />
              <Route path="room-blocks" element={<AdminRoomBlocksPage />} />
              <Route path="amenities" element={<AdminAmenitiesPage />} />
              <Route path="rooms" element={<AdminRoomsPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Route>
          
          {/* ── Receptionist dashboard (STAFF role) ───────────────────── */}
          <Route path="receptionist" element={<RequireRole allowedRoles={['STAFF']}><Outlet /></RequireRole>}>
            <Route element={<ReceptionistDashboardLayout />}>
              <Route index element={<Navigate to="/staff/receptionist/bookings" replace />} />
              <Route path="bookings" element={<ReceptionistBookingsPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Route>

          {/* Fallback for unknown /staff routes */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>

        {/* ═══════════════════════════════════════════════════════════
            FALLBACK - Redirect any unknown routes to home
        ═══════════════════════════════════════════════════════════ */}
        
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
