import { useState } from 'react';
import { Star } from 'lucide-react';
import { cn } from '../../../utils/cn';

interface StarRatingProps {
  value: number; // 0 to 5
  onChange?: (val: number) => void;
  max?: number;
  size?: 'sm' | 'md' | 'lg';
  readonly?: boolean;
  showValueText?: boolean;
  className?: string;
}

const SIZE_MAP = {
  sm: 'h-3.5 w-3.5',
  md: 'h-5 w-5',
  lg: 'h-7 w-7',
};

export default function StarRating({
  value,
  onChange,
  max = 5,
  size = 'md',
  readonly = false,
  showValueText = false,
  className,
}: StarRatingProps) {
  const [hoverValue, setHoverValue] = useState<number | null>(null);

  const activeValue = hoverValue !== null ? hoverValue : value;

  return (
    <div className={cn('inline-flex items-center gap-1', className)}>
      <div className="flex items-center gap-0.5">
        {Array.from({ length: max }, (_, index) => {
          const starNumber = index + 1;
          const isFilled = starNumber <= activeValue;

          return (
            <button
              key={starNumber}
              type="button"
              disabled={readonly}
              onClick={() => {
                if (!readonly && onChange) {
                  onChange(starNumber);
                }
              }}
              onMouseEnter={() => {
                if (!readonly) setHoverValue(starNumber);
              }}
              onMouseLeave={() => {
                if (!readonly) setHoverValue(null);
              }}
              className={cn(
                'transition-transform',
                readonly
                  ? 'cursor-default pointer-events-none'
                  : 'cursor-pointer hover:scale-110 focus:outline-none'
              )}
              aria-label={`${starNumber} out of ${max} stars`}
            >
              <Star
                className={cn(
                  SIZE_MAP[size],
                  isFilled
                    ? 'fill-amber-400 text-amber-400'
                    : 'fill-muted/30 text-muted-foreground/30'
                )}
              />
            </button>
          );
        })}
      </div>

      {showValueText && (
        <span className="text-sm font-semibold text-foreground ml-1.5">
          {value.toFixed(1)}
        </span>
      )}
    </div>
  );
}
