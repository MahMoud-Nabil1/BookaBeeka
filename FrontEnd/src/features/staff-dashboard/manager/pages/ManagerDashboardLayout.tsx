import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { CalendarDays, CreditCard } from 'lucide-react';
import Sidebar, { type NavItem } from '../../../../components/layout/Sidebar';
import Navbar from '../../../../components/layout/Navbar';

const managerNavItems: NavItem[] = [
  { title: 'Bookings', href: '/staff/manager/bookings', icon: CalendarDays },
  { title: 'Payments', href: '/staff/manager/payments', icon: CreditCard },
];

export default function ManagerDashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Navbar onMenuClick={() => setSidebarOpen(true)} showMenuBtn={true} />
      <div className="flex">
        <Sidebar items={managerNavItems} isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
        <main className="flex-1 w-full overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
