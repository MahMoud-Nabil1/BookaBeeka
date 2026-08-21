import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, MapPin, Users, Clock } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import PageLayout from '../../../components/layout/PageLayout';
import SlotPicker from '../components/SlotPicker';
import { MOCK_RESOURCES } from './CatalogPage';
import type { SlotDto } from '../../../types/availability';

export default function ResourceDetailPage() {
  const { resourceId } = useParams();
  const navigate = useNavigate();
  const [selectedSlot, setSelectedSlot] = useState<SlotDto | null>(null);
  
  // Find resource in mock data
  const resource = MOCK_RESOURCES.find(r => r.id === resourceId);

  if (!resource) {
    return (
      <PageLayout>
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <h2 className="text-2xl font-bold mb-2">Resource Not Found</h2>
          <p className="text-muted-foreground mb-6">The resource you are looking for does not exist.</p>
          <Button asChild>
            <Link to="/portal/catalog">Back to Catalog</Link>
          </Button>
        </div>
      </PageLayout>
    );
  }

  const handleBookNow = () => {
    if (!selectedSlot) return;
    
    // TODO: Connect to useCreateBooking hook
    console.log('Initiating booking for:', {
      resourceId,
      slot: selectedSlot
    });
    
    toast.success('Booking initiated! Redirecting to checkout...');
    
    // Simulate flow for now
    setTimeout(() => {
      navigate('/portal/bookings');
    }, 1000);
  };

  return (
    <PageLayout
      action={
        <Button variant="ghost" asChild className="gap-2">
          <Link to="/portal/catalog">
            <ChevronLeft className="h-4 w-4" /> Back to Catalog
          </Link>
        </Button>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Column: Details */}
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl overflow-hidden aspect-[21/9] w-full relative shadow-low">
            <img 
              src={resource.imageUrl} 
              alt={resource.name}
              className="w-full h-full object-cover"
            />
          </div>
          
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-3xl font-bold tracking-tight text-foreground">{resource.name}</h1>
              <Badge variant="secondary" className="text-sm shadow-low bg-surface-container">{resource.resourceType}</Badge>
            </div>
            
            <p className="text-lg text-muted-foreground mb-6">
              Premium space tailored for your specific needs, fully equipped and ready to use.
            </p>
            
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <Card className="shadow-none border-border bg-surface-container-lowest">
                <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                  <Users className="h-6 w-6 text-primary mb-2" />
                  <span className="text-sm font-medium">Capacity</span>
                  <span className="text-sm text-muted-foreground">Up to {resource.capacity} people</span>
                </CardContent>
              </Card>
              <Card className="shadow-none border-border bg-surface-container-lowest">
                <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                  <Clock className="h-6 w-6 text-primary mb-2" />
                  <span className="text-sm font-medium">Duration</span>
                  <span className="text-sm text-muted-foreground">{resource.durationMinutes} min / slot</span>
                </CardContent>
              </Card>
              <Card className="shadow-none border-border bg-surface-container-lowest sm:col-span-2 md:col-span-1">
                <CardContent className="p-4 flex flex-col items-center justify-center text-center">
                  <MapPin className="h-6 w-6 text-primary mb-2" />
                  <span className="text-sm font-medium">Location</span>
                  <span className="text-sm text-muted-foreground">{resource.branchId}</span>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
        
        {/* Right Column: Booking Widget */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24 shadow-raised border-border">
            <div className="p-6 border-b border-border bg-surface-container-lowest rounded-t-xl">
              <div className="text-3xl font-bold text-primary">
                {resource.price} <span className="text-lg font-normal text-muted-foreground">{resource.currency}</span>
              </div>
              <p className="text-sm text-muted-foreground">per {resource.durationMinutes} min slot</p>
            </div>
            
            <CardContent className="p-6">
              <SlotPicker 
                tenantId={resource.tenantId}
                branchId={resource.branchId}
                resourceId={resource.id}
                selectedSlot={selectedSlot}
                onSlotSelect={setSelectedSlot}
              />
              
              <Button 
                className="w-full mt-6 h-12 text-md shadow-low"
                disabled={!selectedSlot}
                onClick={handleBookNow}
              >
                {selectedSlot ? 'Book Now' : 'Select a time slot'}
              </Button>
            </CardContent>
          </Card>
        </div>
        
      </div>
    </PageLayout>
  );
}
