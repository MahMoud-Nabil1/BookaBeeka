import { Link } from 'react-router-dom';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Users, Clock, MapPin } from 'lucide-react';
import PageLayout from '../../../components/layout/PageLayout';

// Temporary mock data until the GET /api/catalog/resources endpoint is implemented
export const MOCK_RESOURCES = [
  {
    id: 'res-101',
    tenantId: 'tenant-vip',
    branchId: 'branch-1',
    name: 'Executive Boardroom',
    resourceType: 'ROOM',
    capacity: 12,
    price: 50,
    currency: 'USD',
    durationMinutes: 60,
    imageUrl: 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80',
    isActive: true,
  },
  {
    id: 'res-102',
    tenantId: 'tenant-vip',
    branchId: 'branch-1',
    name: 'Private Studio Workspace',
    resourceType: 'DESK',
    capacity: 1,
    price: 15,
    currency: 'USD',
    durationMinutes: 60,
    imageUrl: 'https://images.unsplash.com/photo-1527192491265-7e15c55b1ed2?auto=format&fit=crop&w=800&q=80',
    isActive: true,
  },
  {
    id: 'res-103',
    tenantId: 'tenant-health',
    branchId: 'branch-2',
    name: 'Massage Therapy Room',
    resourceType: 'ROOM',
    capacity: 1,
    price: 80,
    currency: 'USD',
    durationMinutes: 45,
    imageUrl: 'https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=800&q=80',
    isActive: true,
  },
];

export default function CatalogPage() {
  return (
    <PageLayout title="Browse Catalog">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {MOCK_RESOURCES.map((resource) => (
          <Card key={resource.id} className="overflow-hidden flex flex-col group border-border">
            <div className="aspect-[16/9] w-full overflow-hidden relative">
              <img 
                src={resource.imageUrl} 
                alt={resource.name}
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
              />
              <Badge className="absolute top-3 right-3 shadow-low bg-background/90 text-foreground hover:bg-background">
                {resource.resourceType}
              </Badge>
            </div>
            
            <CardHeader className="pb-3">
              <CardTitle className="text-xl line-clamp-1">{resource.name}</CardTitle>
            </CardHeader>
            
            <CardContent className="pb-4 flex-1">
              <div className="space-y-2 text-sm text-muted-foreground">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4" />
                  <span>Up to {resource.capacity} people</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  <span>{resource.durationMinutes} minutes per slot</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4" />
                  <span>{resource.branchId}</span>
                </div>
              </div>
            </CardContent>
            
            <CardFooter className="pt-0 border-t border-border mt-auto flex items-center justify-between p-4">
              <div className="font-semibold text-lg text-primary">
                {resource.price} {resource.currency}
              </div>
              <Button asChild className="rounded-full shadow-low hover:shadow-raised transition-shadow">
                <Link to={`/portal/catalog/${resource.id}`}>
                  View Availability
                </Link>
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </PageLayout>
  );
}
