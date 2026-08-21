import { Outlet } from 'react-router-dom';
import Navbar from '../../../components/layout/Navbar';

export default function CustomerPortalLayout() {
  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Navbar />
      <main className="flex-1 w-full flex flex-col">
        <Outlet />
      </main>
    </div>
  );
}
