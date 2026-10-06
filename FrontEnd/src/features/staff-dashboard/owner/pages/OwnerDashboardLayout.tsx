import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, CalendarDays, CreditCard, Tag, DoorOpen, Users } from 'lucide-react';
import Sidebar, { type NavItem } from '../../../../components/layout/Sidebar';
import Navbar from '../../../../components/layout/Navbar';

const ownerNavItems: NavItem[] = [
  { title: 'Overview',       href: '/staff/owner/overview',       icon: LayoutDashboard },
  { title: 'Bookings',       href: '/staff/owner/bookings',       icon: CalendarDays },
  { title: 'Payments',       href: '/staff/owner/payments',       icon: CreditCard },
  { title: 'Rooms',          href: '/staff/owner/rooms',          icon: DoorOpen },
  { title: 'Amenities',      href: '/staff/owner/amenities',      icon: Tag },
  { title: 'Manage Admins',  href: '/staff/owner/admins',         icon: Users },
];

export default function OwnerDashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Navbar onMenuClick={() => setSidebarOpen(true)} showMenuBtn={true} />
      <div className="flex">
        <Sidebar items={ownerNavItems} isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
        <main className="flex-1 w-full overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
