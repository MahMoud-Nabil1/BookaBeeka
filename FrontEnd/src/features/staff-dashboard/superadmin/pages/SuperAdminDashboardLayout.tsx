import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, Building2, Users, DoorOpen, CalendarDays, CreditCard } from 'lucide-react';
import Sidebar, { type NavItem } from '../../../../components/layout/Sidebar';
import Navbar from '../../../../components/layout/Navbar';

const superAdminNavItems: NavItem[] = [
  { title: 'Overview',  href: '/staff/superadmin/overview',  icon: LayoutDashboard },
  { title: 'Hotels',    href: '/staff/superadmin/hotels',    icon: Building2 },
  { title: 'Customers', href: '/staff/superadmin/customers', icon: Users },
  { title: 'All Rooms', href: '/staff/superadmin/rooms',     icon: DoorOpen },
  { title: 'Bookings',  href: '/staff/superadmin/bookings',  icon: CalendarDays },
  { title: 'Payments',  href: '/staff/superadmin/payments',  icon: CreditCard },
];

export default function SuperAdminDashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Navbar onMenuClick={() => setSidebarOpen(true)} showMenuBtn={true} />
      <div className="flex">
        <Sidebar items={superAdminNavItems} isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
        <main className="flex-1 w-full overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
