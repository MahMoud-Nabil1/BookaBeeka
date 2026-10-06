import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { LogOut, User, Menu, Wallet, UserCircle, Search, X } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '../../redux/hooks';
import { selectIsAuthenticated, selectUserType, selectRole } from '../../redux/selectors/authSelectors';
import { logout } from '../../redux/slices/authSlice';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { NotificationBell } from '../../features/notifications/components/NotificationBell';

interface NavbarProps {
  onMenuClick?: () => void;
  showMenuBtn?: boolean;
}

export default function Navbar({ onMenuClick, showMenuBtn = false }: NavbarProps) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const userType = useAppSelector(selectUserType);
  const role = useAppSelector(selectRole);

  const [searchQuery, setSearchQuery] = useState('');

  // Sync search input with URL query param when on the catalog page
  useEffect(() => {
    if (location.pathname.includes('/portal/rooms') || location.pathname.includes('/portal/catalog')) {
      setSearchQuery(searchParams.get('q') || '');
    }
  }, [location.pathname, searchParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = searchQuery.trim();
    if (trimmed) {
      navigate(`/portal/rooms?q=${encodeURIComponent(trimmed)}`);
    } else {
      navigate('/portal/rooms');
    }
  };

  const clearSearch = () => {
    setSearchQuery('');
    if (location.pathname.includes('/portal/rooms')) {
      navigate('/portal/rooms');
    }
  };

  const handleLogout = () => {
    dispatch(logout());
    if (userType === 'STAFF') {
      navigate('/login/owner');
    } else {
      navigate('/login/customer');
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 shadow-low">
      <div className="container flex h-16 items-center justify-between px-4 sm:px-6 lg:px-8 mx-auto max-w-7xl">
        <div className="flex items-center gap-4">
          {showMenuBtn && (
            <Button variant="ghost" size="icon" className="md:hidden" onClick={onMenuClick}>
              <Menu className="h-5 w-5" />
              <span className="sr-only">Toggle menu</span>
            </Button>
          )}
          
          <Link to="/" className="flex items-center gap-2">
            {/* Brand Logo */}
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <span className="font-bold text-white leading-none">B</span>
            </div>
            <span className="hidden font-bold sm:inline-block text-xl tracking-tight text-foreground">
              BookaBeeka
            </span>
          </Link>
        </div>

        {/* Search Bar - centered */}
        {isAuthenticated && userType === 'CUSTOMER' && (
          <form onSubmit={handleSearch} className="hidden sm:flex flex-1 max-w-md mx-4">
            <div className="relative w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search rooms, hotels..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 pr-9 h-9 w-full bg-muted/50 border-transparent focus:bg-background focus:border-input"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={clearSearch}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  <X className="h-4 w-4" />
                  <span className="sr-only">Clear search</span>
                </button>
              )}
            </div>
          </form>
        )}

        <div className="flex items-center gap-4">
          {!isAuthenticated ? (
            <div className="flex items-center gap-2">
              <Button variant="ghost" asChild>
                <Link to="/login/customer">Log in</Link>
              </Button>
              <Button asChild>
                <Link to="/register">Sign up</Link>
              </Button>
            </div>
          ) : (
            <>
              {/* Mobile search button (visible on small screens where search bar is hidden) */}
              {userType === 'CUSTOMER' && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="sm:hidden"
                  onClick={() => navigate('/portal/rooms')}
                >
                  <Search className="h-5 w-5" />
                  <span className="sr-only">Search rooms</span>
                </Button>
              )}
              {/* Show notification bell only for customers */}
              {userType === 'CUSTOMER' && <NotificationBell />}
              
              <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative h-10 w-10 rounded-full">
                  <Avatar className="h-10 w-10 border border-border">
                    <AvatarFallback className="bg-primary/10 text-primary">
                      {role ? role.charAt(0) : 'U'}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-56" align="end" forceMount>
                <DropdownMenuLabel className="font-normal">
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-medium leading-none">
                      {userType === 'STAFF' ? `Staff (${role})` : 'Customer'}
                    </p>
                    <p className="text-xs leading-none text-muted-foreground">
                      Manage your account
                    </p>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link to="/portal/profile" className="cursor-pointer">
                    <UserCircle className="mr-2 h-4 w-4" />
                    <span>Profile</span>
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link to={userType === 'STAFF' ? '/staff' : '/portal/rooms'} className="cursor-pointer">
                    <User className="mr-2 h-4 w-4" />
                    <span>{userType === 'STAFF' ? 'Dashboard' : 'Browse Rooms'}</span>
                  </Link>
                </DropdownMenuItem>
                
                {userType === 'CUSTOMER' && (
                  <DropdownMenuItem asChild>
                    <Link to="/portal/wallet" className="cursor-pointer">
                      <Wallet className="mr-2 h-4 w-4" />
                      <span>Wallet</span>
                    </Link>
                  </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-destructive focus:bg-destructive focus:text-destructive-foreground">
                  <LogOut className="mr-2 h-4 w-4" />
                  <span>Log out</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
