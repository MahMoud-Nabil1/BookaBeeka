import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, CalendarDays, CreditCard, Clock, CalendarX2, Tag, DoorOpen } from 'lucide-react';
import Sidebar, { type NavItem } from '../../../../components/layout/Sidebar';
import Navbar from '../../../../components/layout/Navbar';

// Navigation for ADMIN role only. OWNER has its own layout at owner/pages/OwnerDashboardLayout.tsx
const adminNavItems: NavItem[] = [
  { title: 'Overview', href: '/staff/admin/overview', icon: LayoutDashboard },
  { title: 'Bookings', href: '/staff/admin/bookings', icon: CalendarDays },
  { title: 'Payments', href: '/staff/admin/payments', icon: CreditCard },
  { title: 'Rooms', href: '/staff/admin/rooms', icon: DoorOpen },
  { title: 'Schedule', href: '/staff/admin/schedule', icon: Clock },
  { title: 'Room Blocks', href: '/staff/admin/room-blocks', icon: CalendarX2 },
  { title: 'Amenities', href: '/staff/admin/amenities', icon: Tag },
];

export default function AdminDashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Navbar onMenuClick={() => setSidebarOpen(true)} showMenuBtn={true} />
      <div className="flex">
        <Sidebar items={adminNavItems} isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />
        <main className="flex-1 w-full overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
