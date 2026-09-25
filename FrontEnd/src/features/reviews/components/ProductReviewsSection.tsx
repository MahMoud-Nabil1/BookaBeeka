import { useState } from 'react';
import { Loader2, MessageSquare, PenLine, Star, Filter, ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useServiceReviews } from '../hooks/useReviews';
import ReviewSummary from './ReviewSummary';
import ReviewCard from './ReviewCard';
import AddReviewModal from './AddReviewModal';

interface ProductReviewsSectionProps {
  serviceId: string;
  tenantId: string;
  roomName?: string;
  className?: string;
}

export default function ProductReviewsSection({
  serviceId,
  tenantId,
  roomName,
  className,
}: ProductReviewsSectionProps) {
  const [page, setPage] = useState<number>(0);
  const [filterRating, setFilterRating] = useState<string>('all');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  const { data, isLoading, isError } = useServiceReviews(
    serviceId,
    tenantId,
    page,
    10
  );

  const rawReviews = data?.content || [];
  const totalReviews = data?.totalElements || 0;

  // Filter reviews client-side by star rating if requested
  const filteredReviews =
    filterRating === 'all'
      ? rawReviews
      : rawReviews.filter((r) => Math.round(r.rating) === parseInt(filterRating, 10));

  return (
    <div className={`space-y-6 pt-6 border-t border-border ${className || ''}`}>
      {/* Header & Write Review Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
            <MessageSquare className="h-6 w-6 text-primary" />
            Guest Reviews
          </h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            Verified opinions and experiences from previous guests.
          </p>
        </div>

        <Button
          onClick={() => setIsModalOpen(true)}
          className="gap-2 shadow-low hover:shadow-raised transition-shadow self-start sm:self-auto"
        >
          <PenLine className="h-4 w-4" />
          Write a Review
        </Button>
      </div>

      {/* Loading state */}
      {isLoading ? (
        <div className="flex items-center justify-center py-16 text-muted-foreground">
          <Loader2 className="h-7 w-7 animate-spin mr-3 text-primary" />
          <span>Loading guest reviews...</span>
        </div>
      ) : isError ? (
        <div className="p-6 rounded-xl border border-destructive/20 bg-destructive/5 text-center text-sm text-destructive">
          Could not load reviews at this time.
        </div>
      ) : rawReviews.length === 0 ? (
        /* Empty State */
        <div className="text-center py-12 px-4 rounded-xl border border-dashed border-border bg-card/50 space-y-4">
          <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center mx-auto text-primary">
            <Star className="h-6 w-6" />
          </div>
          <div className="space-y-1">
            <h3 className="font-semibold text-base text-foreground">
              No reviews yet
            </h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              Be the first guest to share feedback after your stay in {roomName || 'this room'}!
            </p>
          </div>
          <Button
            variant="outline"
            onClick={() => setIsModalOpen(true)}
            className="gap-2 text-sm"
          >
            <PenLine className="h-4 w-4" />
            Write the First Review
          </Button>
        </div>
      ) : (
        /* Reviews Content */
        <div className="space-y-6">
          {/* Rating Summary Bar */}
          <ReviewSummary reviews={rawReviews} totalElements={totalReviews} />

          {/* Filter Bar */}
          <div className="flex items-center justify-between gap-4 pt-2">
            <div className="text-sm font-medium text-foreground">
              Showing {filteredReviews.length} of {totalReviews} {totalReviews === 1 ? 'review' : 'reviews'}
            </div>

            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-muted-foreground" />
              <Select value={filterRating} onValueChange={setFilterRating}>
                <SelectTrigger className="w-[140px] h-9 text-xs">
                  <SelectValue placeholder="All Ratings" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Ratings</SelectItem>
                  <SelectItem value="5">5 Stars only</SelectItem>
                  <SelectItem value="4">4 Stars only</SelectItem>
                  <SelectItem value="3">3 Stars only</SelectItem>
                  <SelectItem value="2">2 Stars only</SelectItem>
                  <SelectItem value="1">1 Star only</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Review List */}
          <div className="space-y-4">
            {filteredReviews.length === 0 ? (
              <div className="p-8 text-center text-sm text-muted-foreground border border-dashed border-border rounded-lg">
                No reviews found matching the selected star rating.
              </div>
            ) : (
              filteredReviews.map((review) => (
                <ReviewCard key={review.id} review={review} />
              ))
            )}
          </div>

          {/* Pagination Controls */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t border-border">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                disabled={data.first || page === 0}
                className="gap-1 text-xs"
              >
                <ChevronLeft className="h-4 w-4" /> Previous
              </Button>
              <span className="text-xs text-muted-foreground">
                Page {page + 1} of {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((prev) => prev + 1)}
                disabled={data.last}
                className="gap-1 text-xs"
              >
                Next <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Add Review Dialog Modal */}
      <AddReviewModal
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        serviceId={serviceId}
        tenantId={tenantId}
        roomName={roomName}
      />
    </div>
  );
}
