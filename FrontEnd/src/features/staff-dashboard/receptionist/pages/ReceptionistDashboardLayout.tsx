import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { CalendarDays } from 'lucide-react';
import Sidebar, { type NavItem } from '../../../../components/layout/Sidebar';
import Navbar from '../../../../components/layout/Navbar';

const receptionistNavItems: NavItem[] = [
  { title: 'Bookings', href: '/staff/receptionist/bookings', icon: CalendarDays },
];

export default function ReceptionistDashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Navbar onMenuClick={() => setSidebarOpen(true)} showMenuBtn={true} />
      <div className="flex">
        <Sidebar items={receptionistNavItems} isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
        <main className="flex-1 w-full overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
