import { type ReactNode } from 'react';

interface PageLayoutProps {
  children: ReactNode;
  title?: string;
  action?: ReactNode;
}

export default function PageLayout({ children, title, action }: PageLayoutProps) {
  return (
    <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-500">
      {(title || action) && (
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
          {title && (
            <h1 className="text-3xl font-bold tracking-tight text-foreground">
              {title}
            </h1>
          )}
          {action && <div>{action}</div>}
        </div>
      )}
      <main>{children}</main>
    </div>
  );
}
