import { Link, useLocation } from 'react-router-dom';
import { type LucideIcon } from 'lucide-react';
import { cn } from '../../utils';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent } from '@/components/ui/sheet';

export interface NavItem {
  title: string;
  href: string;
  icon: LucideIcon;
}

interface SidebarProps {
  items: NavItem[];
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
}

export default function Sidebar({ items, isOpen, setIsOpen }: SidebarProps) {
  const location = useLocation();

  const NavLinks = () => (
    <nav className="flex flex-col gap-2 p-4">
      {items.map((item) => {
        const isActive = location.pathname.startsWith(item.href);
        return (
          <Button
            key={item.href}
            variant={isActive ? 'secondary' : 'ghost'}
            className={cn(
              'justify-start w-full',
              isActive ? 'bg-secondary font-medium' : 'font-normal text-muted-foreground'
            )}
            asChild
            onClick={() => setIsOpen(false)} // Close mobile sheet on click
          >
            <Link to={item.href}>
              <item.icon className="mr-3 h-5 w-5" />
              {item.title}
            </Link>
          </Button>
        );
      })}
    </nav>
  );

  return (
    <>
      {/* Mobile Sidebar (Sheet) */}
      <Sheet open={isOpen} onOpenChange={setIsOpen}>
        <SheetContent side="left" className="w-64 p-0 border-r border-border">
          <div className="h-16 flex items-center px-6 border-b border-border">
            <span className="font-bold text-lg tracking-tight text-foreground">Menu</span>
          </div>
          <NavLinks />
        </SheetContent>
      </Sheet>

      {/* Desktop Sidebar (Static) */}
      <aside className="hidden md:flex w-64 flex-col border-r border-border bg-background/50 min-h-[calc(100vh-4rem)]">
        <NavLinks />
      </aside>
    </>
  );
}
