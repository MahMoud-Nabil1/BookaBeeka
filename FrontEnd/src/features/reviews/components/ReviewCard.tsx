import { ShieldCheck, Building2, User } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import StarRating from './StarRating';
import type { ReviewDto } from '../../../types/review';

interface ReviewCardProps {
  review: ReviewDto;
}

function formatDate(dateString: string): string {
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(date);
  } catch {
    return dateString;
  }
}

export default function ReviewCard({ review }: ReviewCardProps) {
  // Build reviewer display name from real data; fall back to "Verified Guest" only when absent
  const firstName = review.customerFirstName?.trim() || '';
  const lastName  = review.customerLastName?.trim()  || '';
  const customerLabel =
    firstName || lastName
      ? `${firstName} ${lastName}`.trim()
      : 'Verified Guest';

  return (
    <Card className="border border-border/70 shadow-none hover:shadow-low transition-shadow bg-card">
      <CardContent className="p-5 space-y-4">
        {/* Reviewer Header */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <Avatar className="h-10 w-10 border border-border/80">
              <AvatarFallback className="bg-primary/10 text-primary font-medium text-sm">
                {firstName ? firstName[0].toUpperCase() : <User className="h-5 w-5 text-primary" />}
              </AvatarFallback>
            </Avatar>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-semibold text-sm text-foreground">
                  {customerLabel}
                </span>
                {review.isVerified && (
                  <Badge
                    variant="outline"
                    className="gap-1 border-primary/30 text-primary text-[11px] font-medium py-0 px-1.5 bg-primary/5"
                  >
                    <ShieldCheck className="h-3 w-3" />
                    Verified Stay
                  </Badge>
                )}
              </div>
              <span className="text-xs text-muted-foreground">
                {formatDate(review.createdAt)}
              </span>
            </div>
          </div>

          <StarRating value={review.rating} readonly size="sm" />
        </div>

        {/* Review Text */}
        {review.comment ? (
          <p className="text-sm text-foreground/90 leading-relaxed whitespace-pre-line">
            {review.comment}
          </p>
        ) : (
          <p className="text-xs italic text-muted-foreground">
            No written comment provided.
          </p>
        )}

        {/* Staff / Hotel Reply (if present) */}
        {review.reply && (
          <div className="mt-3 rounded-lg border-l-4 border-primary bg-muted/40 p-4 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                <Building2 className="h-3.5 w-3.5 text-primary" />
                <span>
                  Response from {review.repliedByRole ? `${review.repliedByRole.replace('_', ' ')}` : 'Management'}
                </span>
              </div>
              {review.repliedAt && (
                <span className="text-[11px] text-muted-foreground">
                  {formatDate(review.repliedAt)}
                </span>
              )}
            </div>
            <p className="text-xs text-foreground/80 leading-relaxed">
              {review.reply}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
