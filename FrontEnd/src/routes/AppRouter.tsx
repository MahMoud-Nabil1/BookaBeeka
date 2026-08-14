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
import ResourceDetailPage from '../features/catalog/pages/ResourceDetailPage';

// Admin dashboard
import AdminDashboardLayout from '../features/staff-dashboard/admin/pages/AdminDashboardLayout';
import AdminOverviewPage from '../features/staff-dashboard/admin/pages/AdminOverviewPage';
import AdminBookingsPage from '../features/staff-dashboard/admin/pages/AdminBookingsPage';
import AdminPaymentsPage from '../features/staff-dashboard/admin/pages/AdminPaymentsPage';
import AdminSchedulePage from '../features/staff-dashboard/admin/pages/AdminSchedulePage';

// Manager dashboard
import ManagerDashboardLayout from '../features/staff-dashboard/manager/pages/ManagerDashboardLayout';
import ManagerBookingsPage from '../features/staff-dashboard/manager/pages/ManagerBookingsPage';
import ManagerPaymentsPage from '../features/staff-dashboard/manager/pages/ManagerPaymentsPage';

// Receptionist dashboard
import ReceptionistDashboardLayout from '../features/staff-dashboard/receptionist/pages/ReceptionistDashboardLayout';
import ReceptionistBookingsPage from '../features/staff-dashboard/receptionist/pages/ReceptionistBookingsPage';

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* ── Public (guest only) ──────────────────────────────────── */}
        <Route element={<GuestOnly><Outlet /></GuestOnly>}>
          <Route path="/login/customer" element={<CustomerLoginPage />} />
          <Route path="/login/staff"    element={<StaffLoginPage />} />
          <Route path="/register"       element={<CustomerRegisterPage />} />
        </Route>

        {/* ── Customer portal ──────────────────────────────────────── */}
        <Route element={<RequireAuth allowedUserType="CUSTOMER"><Outlet /></RequireAuth>}>
          <Route element={<CustomerPortalLayout />}>
            <Route path="/portal/catalog"                   element={<CatalogPage />} />
            <Route path="/portal/catalog/:resourceId"       element={<ResourceDetailPage />} />
            <Route path="/portal/bookings"                  element={<CustomerBookingsPage />} />
            <Route path="/portal/bookings/:bookingId"       element={<BookingDetailPage />} />
            <Route path="/portal/wallet"                    element={<CustomerWalletPage />} />
            {/* Default customer landing */}
            <Route path="/portal" element={<Navigate to="/portal/catalog" replace />} />
          </Route>
        </Route>

        {/* ── Staff: role redirect ──────────────────────────────────── */}
        <Route element={<RequireAuth allowedUserType="STAFF"><Outlet /></RequireAuth>}>
          <Route path="/staff" element={<StaffRoleRedirect />} />

          {/* ADMIN dashboard */}
          <Route element={<RequireRole allowedRoles={['ADMIN']}><Outlet /></RequireRole>}>
            <Route element={<AdminDashboardLayout />}>
              <Route path="/staff/admin/overview"  element={<AdminOverviewPage />} />
              <Route path="/staff/admin/bookings"  element={<AdminBookingsPage />} />
              <Route path="/staff/admin/payments"  element={<AdminPaymentsPage />} />
              <Route path="/staff/admin/schedule"  element={<AdminSchedulePage />} />
              <Route path="/staff/admin" element={<Navigate to="/staff/admin/overview" replace />} />
            </Route>
          </Route>

          {/* MANAGER dashboard */}
          <Route element={<RequireRole allowedRoles={['MANAGER']}><Outlet /></RequireRole>}>
            <Route element={<ManagerDashboardLayout />}>
              <Route path="/staff/manager/bookings" element={<ManagerBookingsPage />} />
              <Route path="/staff/manager/payments" element={<ManagerPaymentsPage />} />
              <Route path="/staff/manager" element={<Navigate to="/staff/manager/bookings" replace />} />
            </Route>
          </Route>

          {/* RECEPTIONIST dashboard */}
          <Route element={<RequireRole allowedRoles={['RECEPTIONIST']}><Outlet /></RequireRole>}>
            <Route element={<ReceptionistDashboardLayout />}>
              <Route path="/staff/receptionist/bookings" element={<ReceptionistBookingsPage />} />
              <Route path="/staff/receptionist" element={<Navigate to="/staff/receptionist/bookings" replace />} />
            </Route>
          </Route>
        </Route>

        {/* ── Public landing page ────────────────────────────────────── */}
        <Route path="/" element={<LandingPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
