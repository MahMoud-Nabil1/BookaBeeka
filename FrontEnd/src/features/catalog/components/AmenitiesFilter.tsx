import { useState } from 'react';
import {
  Sparkles,
  Search,
  Check,
  X,
  Wifi,
  Wind,
  Tv,
  Waves,
  Coffee,
  Car,
  Sun,
  Eye,
  Wine,
  UtensilsCrossed,
  Dumbbell,
  Laptop,
} from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Checkbox } from '@/components/ui/checkbox';
import { cn } from '@/utils';

export interface AmenityStat {
  name: string;
  count: number;
}

interface AmenitiesFilterProps {
  amenities: AmenityStat[];
  selectedAmenities: string[];
  onToggleAmenity: (name: string) => void;
  onClearAmenities: () => void;
  showQuickPills?: boolean;
}

export function getAmenityIcon(name: string, className = 'h-3.5 w-3.5') {
  const lower = name.toLowerCase();
  if (lower.includes('wifi') || lower.includes('internet')) return <Wifi className={className} />;
  if (lower.includes('air') || lower.includes('ac') || lower.includes('cooling') || lower.includes('condition')) return <Wind className={className} />;
  if (lower.includes('tv') || lower.includes('television') || lower.includes('theater')) return <Tv className={className} />;
  if (lower.includes('pool') || lower.includes('swim')) return <Waves className={className} />;
  if (lower.includes('breakfast') || lower.includes('coffee') || lower.includes('dining')) return <Coffee className={className} />;
  if (lower.includes('parking') || lower.includes('car')) return <Car className={className} />;
  if (lower.includes('balcony') || lower.includes('terrace') || lower.includes('patio') || lower.includes('hammock')) return <Sun className={className} />;
  if (lower.includes('view') || lower.includes('sea') || lower.includes('ocean')) return <Eye className={className} />;
  if (lower.includes('bar') || lower.includes('wine') || lower.includes('drink')) return <Wine className={className} />;
  if (lower.includes('kitchen') || lower.includes('cook') || lower.includes('refrigerator')) return <UtensilsCrossed className={className} />;
  if (lower.includes('gym') || lower.includes('fitness')) return <Dumbbell className={className} />;
  if (lower.includes('desk') || lower.includes('work') || lower.includes('laptop')) return <Laptop className={className} />;
  return <Sparkles className={cn(className, 'text-primary/70')} />;
}

export default function AmenitiesFilter({
  amenities,
  selectedAmenities,
  onToggleAmenity,
  onClearAmenities,
  showQuickPills = true,
}: AmenitiesFilterProps) {
  const [open, setOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const isFilterActive = selectedAmenities.length > 0;

  // Filter amenities list by search query
  const filteredAmenities = amenities.filter((item) =>
    item.name.toLowerCase().includes(searchQuery.trim().toLowerCase())
  );

  // Top popular amenities for quick pills (take up to 4 most common)
  const popularPills = amenities.slice(0, 4);

  return (
    <div className="flex items-center flex-wrap gap-2">
      {/* Popover trigger button */}
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant={isFilterActive ? 'default' : 'outline'}
            size="sm"
            className={cn(
              'h-9 rounded-full px-3.5 font-medium transition-all gap-2 text-xs sm:text-sm',
              isFilterActive
                ? 'bg-primary text-primary-foreground shadow-sm hover:bg-primary/90'
                : 'hover:bg-muted/70 border-border text-foreground'
            )}
          >
            <Sparkles className="h-3.5 w-3.5 shrink-0" />
            <span>Room Amenities</span>
            {isFilterActive && (
              <Badge
                variant="secondary"
                className="ml-0.5 rounded-full px-1.5 py-0 text-[10px] font-semibold bg-primary-foreground/20 text-primary-foreground border-none"
              >
                {selectedAmenities.length}
              </Badge>
            )}
          </Button>
        </PopoverTrigger>

        <PopoverContent
          align="start"
          sideOffset={8}
          className="w-[320px] sm:w-[360px] p-4 rounded-2xl shadow-xl border border-border bg-card text-card-foreground space-y-4"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-border pb-3">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary" />
              <div>
                <h3 className="font-semibold text-sm text-foreground">Room Amenities</h3>
                <p className="text-[11px] text-muted-foreground">What comes with the room</p>
              </div>
            </div>
            {isFilterActive && (
              <button
                onClick={onClearAmenities}
                className="text-xs text-muted-foreground hover:text-primary transition-colors flex items-center gap-1"
              >
                <X className="h-3 w-3" /> Clear
              </button>
            )}
          </div>

          {/* Search Bar */}
          <div className="relative">
            <Search className="h-3.5 w-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search amenities (WiFi, Pool, AC...)"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 h-8 text-xs rounded-lg"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>

          {/* Amenities List */}
          <div className="max-h-60 overflow-y-auto space-y-1 pr-1">
            {filteredAmenities.length === 0 ? (
              <div className="py-6 text-center text-xs text-muted-foreground">
                No amenities found matching &quot;{searchQuery}&quot;
              </div>
            ) : (
              filteredAmenities.map((item) => {
                const isSelected = selectedAmenities.includes(item.name);
                return (
                  <div
                    key={item.name}
                    onClick={() => onToggleAmenity(item.name)}
                    className={cn(
                      'flex items-center justify-between p-2 rounded-lg cursor-pointer text-xs transition-colors',
                      isSelected
                        ? 'bg-primary/10 text-primary font-medium'
                        : 'hover:bg-muted text-foreground'
                    )}
                  >
                    <div className="flex items-center gap-2.5">
                      <Checkbox
                        checked={isSelected}
                        onCheckedChange={() => onToggleAmenity(item.name)}
                        className="rounded"
                      />
                      <div className="flex items-center gap-2">
                        {getAmenityIcon(item.name, 'h-3.5 w-3.5 text-muted-foreground')}
                        <span>{item.name}</span>
                      </div>
                    </div>
                    <span className="text-[11px] text-muted-foreground font-mono">
                      {item.count}
                    </span>
                  </div>
                );
              })
            )}
          </div>

          {/* Footer actions */}
          <div className="flex items-center justify-between gap-2 border-t border-border pt-3">
            <span className="text-xs text-muted-foreground">
              {selectedAmenities.length} selected
            </span>
            <Button
              size="sm"
              onClick={() => setOpen(false)}
              className="text-xs rounded-full px-5 h-8 font-medium shadow-sm"
            >
              Done
            </Button>
          </div>
        </PopoverContent>
      </Popover>

      {/* Quick Pills for most common amenities */}
      {showQuickPills && popularPills.length > 0 && (
        <div className="hidden sm:flex items-center gap-1.5 flex-wrap">
          {popularPills.map((item) => {
            const isSelected = selectedAmenities.includes(item.name);
            return (
              <button
                key={item.name}
                type="button"
                onClick={() => onToggleAmenity(item.name)}
                className={cn(
                  'h-9 px-3 rounded-full text-xs font-medium border transition-all flex items-center gap-1.5',
                  isSelected
                    ? 'bg-primary text-primary-foreground border-primary shadow-sm'
                    : 'border-border bg-card hover:bg-muted/70 text-muted-foreground hover:text-foreground'
                )}
              >
                {getAmenityIcon(item.name, 'h-3 w-3')}
                <span>{item.name}</span>
                {isSelected && <Check className="h-3 w-3 ml-0.5" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
