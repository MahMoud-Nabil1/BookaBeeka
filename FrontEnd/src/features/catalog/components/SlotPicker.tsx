import { useState } from 'react';
import { format } from 'date-fns';
import { Calendar as CalendarIcon, Loader2 } from 'lucide-react';
import { cn } from '../../../utils';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { useAvailability } from '../hooks/useAvailability';
import type { SlotDto } from '../../../types/availability';

interface SlotPickerProps {
  tenantId: string;
  branchId: string;
  resourceId: string;
  onSlotSelect: (slot: SlotDto) => void;
  selectedSlot: SlotDto | null;
}

export default function SlotPicker({ tenantId, branchId, resourceId, onSlotSelect, selectedSlot }: SlotPickerProps) {
  const [date, setDate] = useState<Date>(new Date());
  
  // Convert JS Date to YYYY-MM-DD for the API
  const dateStr = format(date, 'yyyy-MM-dd');

  const { data: slots, isLoading, isError } = useAvailability({
    tenantId,
    branchId,
    resourceId,
    date: dateStr,
  });

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
          Select Date
        </label>
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              className={cn(
                'w-full justify-start text-left font-normal border-border bg-surface hover:bg-surface-container',
                !date && 'text-muted-foreground'
              )}
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {date ? format(date, 'PPP') : <span>Pick a date</span>}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={date}
              onSelect={(d) => d && setDate(d)}
              autoFocus
              disabled={(date) => date < new Date(new Date().setHours(0, 0, 0, 0))} // disable past dates
            />
          </PopoverContent>
        </Popover>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium leading-none">
          Available Time Slots
        </label>
        
        {isLoading ? (
          <div className="flex justify-center items-center py-8 text-muted-foreground">
            <Loader2 className="h-6 w-6 animate-spin" />
            <span className="ml-2 text-sm">Loading slots...</span>
          </div>
        ) : isError ? (
          <div className="p-4 rounded-md bg-destructive/10 text-destructive text-sm text-center">
            Failed to load availability. Please try again.
          </div>
        ) : !slots || slots.length === 0 ? (
          <div className="p-8 text-center border border-dashed border-border rounded-lg text-muted-foreground bg-surface/50">
            No available slots for this date.
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 max-h-[300px] overflow-y-auto p-1">
            {slots.map((slot, i) => {
              // Parse ISO string to display time
              const startTime = new Date(slot.start);
              const isSelected = selectedSlot?.start === slot.start;
              
              return (
                <Button
                  key={i}
                  variant={isSelected ? 'default' : 'outline'}
                  className={cn(
                    'w-full transition-all duration-200',
                    isSelected && 'shadow-low scale-[1.02]'
                  )}
                  onClick={() => onSlotSelect(slot)}
                >
                  {format(startTime, 'h:mm a')}
                </Button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
