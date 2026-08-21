import { Link, useNavigate } from 'react-router-dom';
import { LogOut, User, Menu, Wallet } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '../../redux/hooks';
import { selectIsAuthenticated, selectUserType, selectRole } from '../../redux/selectors/authSelectors';
import { logout } from '../../redux/slices/authSlice';

import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

interface NavbarProps {
  onMenuClick?: () => void;
  showMenuBtn?: boolean;
}

export default function Navbar({ onMenuClick, showMenuBtn = false }: NavbarProps) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const userType = useAppSelector(selectUserType);
  const role = useAppSelector(selectRole);

  const handleLogout = () => {
    dispatch(logout());
    if (userType === 'STAFF') {
      navigate('/login/staff');
    } else {
      navigate('/login/customer');
    }
  };

  const getPortalLink = () => {
    if (!isAuthenticated) return '/';
    if (userType === 'STAFF') return '/staff';
    return '/portal';
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
          
          <Link to={getPortalLink()} className="flex items-center gap-2">
            {/* Brand Logo */}
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <span className="font-bold text-white leading-none">B</span>
            </div>
            <span className="hidden font-bold sm:inline-block text-xl tracking-tight text-foreground">
              BookaBeeka
            </span>
          </Link>
        </div>

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
                  <Link to={userType === 'STAFF' ? '/staff' : '/portal'} className="cursor-pointer">
                    <User className="mr-2 h-4 w-4" />
                    <span>Dashboard</span>
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
          )}
        </div>
      </div>
    </header>
  );
}
