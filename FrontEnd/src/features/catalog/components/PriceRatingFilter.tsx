import { useState, useEffect } from 'react';
import { SlidersHorizontal, Star, DollarSign, ArrowUpDown, X, Check } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/utils';

interface PriceRatingFilterProps {
  minPrice: number | null;
  maxPrice: number | null;
  onPriceChange: (min: number | null, max: number | null) => void;
  minRating: number | null;
  onRatingChange: (rating: number | null) => void;
  sortBy: string;
  onSortChange: (sort: string) => void;
  onReset: () => void;
  highestPrice?: number;
}

const RATING_OPTIONS = [
  { value: null, label: 'Any Rating' },
  { value: 4.8, label: '4.8+ Exceptional' },
  { value: 4.5, label: '4.5+ Very Good' },
  { value: 4.0, label: '4.0+ Good' },
  { value: 3.5, label: '3.5+ Satisfactory' },
];

const PRICE_PRESETS = [
  { label: 'All', min: null, max: null },
  { label: 'Under $150', min: null, max: 150 },
  { label: '$150 - $300', min: 150, max: 300 },
  { label: '$300 - $600', min: 300, max: 600 },
  { label: '$600+', min: 600, max: null },
];

const SORT_OPTIONS = [
  { value: 'recommended', label: 'Recommended' },
  { value: 'price-asc', label: 'Price: Low to High' },
  { value: 'price-desc', label: 'Price: High to Low' },
  { value: 'rating-desc', label: 'Highest Rated' },
];

export default function PriceRatingFilter({
  minPrice,
  maxPrice,
  onPriceChange,
  minRating,
  onRatingChange,
  sortBy,
  onSortChange,
  onReset,
  highestPrice = 1000,
}: PriceRatingFilterProps) {
  const [open, setOpen] = useState(false);
  const [localMinPrice, setLocalMinPrice] = useState<string>(minPrice !== null ? String(minPrice) : '');
  const [localMaxPrice, setLocalMaxPrice] = useState<string>(maxPrice !== null ? String(maxPrice) : '');
  const [localRating, setLocalRating] = useState<number | null>(minRating);
  const [localSort, setLocalSort] = useState<string>(sortBy);

  // Sync internal state when parent props change
  useEffect(() => {
    setLocalMinPrice(minPrice !== null ? String(minPrice) : '');
    setLocalMaxPrice(maxPrice !== null ? String(maxPrice) : '');
    setLocalRating(minRating);
    setLocalSort(sortBy);
  }, [minPrice, maxPrice, minRating, sortBy]);

  const isFilterActive = minPrice !== null || maxPrice !== null || minRating !== null || sortBy !== 'recommended';

  const handleApply = () => {
    const parsedMin = localMinPrice ? Math.max(0, Number(localMinPrice)) : null;
    const parsedMax = localMaxPrice ? Math.max(0, Number(localMaxPrice)) : null;
    onPriceChange(parsedMin, parsedMax);
    onRatingChange(localRating);
    onSortChange(localSort);
    setOpen(false);
  };

  const handleReset = () => {
    setLocalMinPrice('');
    setLocalMaxPrice('');
    setLocalRating(null);
    setLocalSort('recommended');
    onReset();
    setOpen(false);
  };

  const handlePresetClick = (presetMin: number | null, presetMax: number | null) => {
    setLocalMinPrice(presetMin !== null ? String(presetMin) : '');
    setLocalMaxPrice(presetMax !== null ? String(presetMax) : '');
  };

  // Build button summary label
  const getSummaryBadge = () => {
    const parts: string[] = [];
    if (minPrice !== null && maxPrice !== null) {
      parts.push(`$${minPrice}-$${maxPrice}`);
    } else if (minPrice !== null) {
      parts.push(`>$${minPrice}`);
    } else if (maxPrice !== null) {
      parts.push(`<$${maxPrice}`);
    }

    if (minRating !== null) {
      parts.push(`${minRating}+★`);
    }

    if (sortBy === 'price-asc') parts.push('Price ↑');
    if (sortBy === 'price-desc') parts.push('Price ↓');
    if (sortBy === 'rating-desc') parts.push('Rating ↓');

    return parts.join(' • ');
  };

  return (
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
          <SlidersHorizontal className="h-3.5 w-3.5 shrink-0" />
          <span>Price & Rating</span>
          {isFilterActive && (
            <Badge
              variant="secondary"
              className="ml-0.5 rounded-full px-1.5 py-0 text-[10px] font-semibold bg-primary-foreground/20 text-primary-foreground border-none"
            >
              {getSummaryBadge() || 'Active'}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>

      <PopoverContent
        align="start"
        sideOffset={8}
        className="w-[340px] sm:w-[380px] p-5 rounded-2xl shadow-xl border border-border bg-card text-card-foreground space-y-5"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border pb-3">
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-primary" />
            <h3 className="font-semibold text-sm text-foreground">Price & Rating Filter</h3>
          </div>
          {isFilterActive && (
            <button
              onClick={handleReset}
              className="text-xs text-muted-foreground hover:text-primary transition-colors flex items-center gap-1"
            >
              <X className="h-3 w-3" /> Reset
            </button>
          )}
        </div>

        {/* 1. Price Range */}
        <div className="space-y-3">
          <label className="text-xs font-semibold text-foreground uppercase tracking-wider block">
            Price Per Night (USD)
          </label>

          {/* Quick presets */}
          <div className="flex flex-wrap gap-1.5">
            {PRICE_PRESETS.map((preset) => {
              const isSelected =
                (preset.min === null ? localMinPrice === '' : localMinPrice === String(preset.min)) &&
                (preset.max === null ? localMaxPrice === '' : localMaxPrice === String(preset.max));

              return (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => handlePresetClick(preset.min, preset.max)}
                  className={cn(
                    'px-2.5 py-1 text-xs rounded-full border transition-all',
                    isSelected
                      ? 'bg-primary text-primary-foreground border-primary font-medium shadow-sm'
                      : 'border-border bg-muted/30 text-muted-foreground hover:bg-muted hover:text-foreground'
                  )}
                >
                  {preset.label}
                </button>
              );
            })}
          </div>

          {/* Custom Min / Max inputs */}
          <div className="grid grid-cols-2 gap-3 items-center">
            <div className="space-y-1">
              <span className="text-[11px] text-muted-foreground">Min Price</span>
              <div className="relative">
                <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-xs text-muted-foreground">$</span>
                <Input
                  type="number"
                  placeholder="0"
                  min="0"
                  value={localMinPrice}
                  onChange={(e) => setLocalMinPrice(e.target.value)}
                  className="pl-6 h-8 text-xs rounded-lg"
                />
              </div>
            </div>

            <div className="space-y-1">
              <span className="text-[11px] text-muted-foreground">Max Price</span>
              <div className="relative">
                <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-xs text-muted-foreground">$</span>
                <Input
                  type="number"
                  placeholder={String(highestPrice)}
                  min="0"
                  value={localMaxPrice}
                  onChange={(e) => setLocalMaxPrice(e.target.value)}
                  className="pl-6 h-8 text-xs rounded-lg"
                />
              </div>
            </div>
          </div>
        </div>

        {/* 2. Rating Threshold */}
        <div className="space-y-2 border-t border-border pt-4">
          <label className="text-xs font-semibold text-foreground uppercase tracking-wider block">
            Guest Rating
          </label>
          <div className="grid grid-cols-2 gap-2">
            {RATING_OPTIONS.map((opt) => {
              const isSelected = localRating === opt.value;
              return (
                <button
                  key={opt.label}
                  type="button"
                  onClick={() => setLocalRating(isSelected && opt.value !== null ? null : opt.value)}
                  className={cn(
                    'flex items-center justify-between px-3 py-1.5 rounded-lg border text-xs text-left transition-all',
                    isSelected
                      ? 'border-amber-500/80 bg-amber-500/10 text-amber-600 dark:text-amber-400 font-medium'
                      : 'border-border bg-card hover:bg-muted/50 text-foreground'
                  )}
                >
                  <div className="flex items-center gap-1.5">
                    {opt.value !== null && <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400 shrink-0" />}
                    <span>{opt.label}</span>
                  </div>
                  {isSelected && <Check className="h-3 w-3 text-amber-500 shrink-0" />}
                </button>
              );
            })}
          </div>
        </div>

        {/* 3. Sort Order */}
        <div className="space-y-2 border-t border-border pt-4">
          <div className="flex items-center gap-1.5">
            <ArrowUpDown className="h-3.5 w-3.5 text-muted-foreground" />
            <label className="text-xs font-semibold text-foreground uppercase tracking-wider">
              Sort By
            </label>
          </div>
          <div className="grid grid-cols-2 gap-1.5">
            {SORT_OPTIONS.map((opt) => {
              const isSelected = localSort === opt.value;
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setLocalSort(opt.value)}
                  className={cn(
                    'px-2.5 py-1.5 rounded-md border text-xs text-center transition-all',
                    isSelected
                      ? 'bg-primary/10 border-primary text-primary font-medium'
                      : 'border-border/60 hover:bg-muted/50 text-muted-foreground hover:text-foreground'
                  )}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Footer actions */}
        <div className="flex items-center justify-between gap-2 border-t border-border pt-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleReset}
            className="text-xs text-muted-foreground h-8"
          >
            Clear All
          </Button>
          <Button
            size="sm"
            onClick={handleApply}
            className="text-xs rounded-full px-5 h-8 font-medium shadow-sm"
          >
            Apply Filters
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
