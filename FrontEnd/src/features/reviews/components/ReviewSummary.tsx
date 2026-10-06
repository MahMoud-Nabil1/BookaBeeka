import StarRating from './StarRating';
import type { ReviewDto } from '../../../types/review';

interface ReviewSummaryProps {
  reviews: ReviewDto[];
  totalElements?: number;
}

export default function ReviewSummary({
  reviews,
  totalElements,
}: ReviewSummaryProps) {
  const count = totalElements !== undefined ? totalElements : reviews.length;

  if (count === 0) {
    return null;
  }

  // Calculate average
  const totalRating = reviews.reduce((acc, r) => acc + (r.rating || 0), 0);
  const average = reviews.length > 0 ? (totalRating / reviews.length) : 5.0;

  // Star distributions
  const starCounts: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  reviews.forEach((r) => {
    const star = Math.min(5, Math.max(1, Math.round(r.rating)));
    starCounts[star] = (starCounts[star] || 0) + 1;
  });

  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center p-6 rounded-xl border border-border bg-card shadow-sm">
      {/* Overall Score */}
      <div className="md:col-span-4 flex flex-col items-center justify-center text-center p-2 border-b md:border-b-0 md:border-r border-border pb-4 md:pb-0">
        <div className="text-4xl font-extrabold text-foreground tracking-tight">
          {average.toFixed(1)}
        </div>
        <div className="my-2">
          <StarRating value={Math.round(average)} readonly size="md" />
        </div>
        <div className="text-xs text-muted-foreground font-medium">
          Based on {count} {count === 1 ? 'verified review' : 'verified reviews'}
        </div>
      </div>

      {/* Rating Breakdown Bars */}
      <div className="md:col-span-8 space-y-2">
        {[5, 4, 3, 2, 1].map((stars) => {
          const starReviews = starCounts[stars] || 0;
          const percentage = reviews.length > 0 ? Math.round((starReviews / reviews.length) * 100) : 0;

          return (
            <div key={stars} className="flex items-center gap-3 text-xs">
              <span className="w-7 text-right font-medium text-muted-foreground">
                {stars} ★
              </span>
              <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                <div
                  className="h-full bg-amber-400 rounded-full transition-all duration-500"
                  style={{ width: `${percentage}%` }}
                />
              </div>
              <span className="w-10 text-right text-muted-foreground font-mono">
                {percentage}%
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
