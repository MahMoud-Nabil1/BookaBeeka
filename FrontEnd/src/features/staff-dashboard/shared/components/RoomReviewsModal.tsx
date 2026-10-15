import { useState } from 'react';
import { Star, Loader2, MessageSquare, ShieldCheck, Building2, User } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import StarRating from '../../../reviews/components/StarRating';
import { useAdminReviews } from '../../../reviews/hooks/useReviews';
import { ReplyReviewDialog } from './ReplyReviewDialog';
import { useAppSelector } from '../../../../redux/hooks';
import { selectRole, selectTenantId } from '../../../../redux/selectors/authSelectors';
import type { ReviewDto } from '../../../../types/review';

interface RoomReviewsModalProps {
  roomId: string;
  roomName: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
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

export function RoomReviewsModal({
  roomId,
  roomName,
  open,
  onOpenChange,
}: RoomReviewsModalProps) {
  const tenantId = useAppSelector(selectTenantId);
  const role = useAppSelector(selectRole);
  // ADMIN and SUPER_ADMIN can reply; OWNER cannot reply.
  const canReply = role === 'ADMIN' || role === 'SUPER_ADMIN';

  const [reviewToReply, setReviewToReply] = useState<ReviewDto | null>(null);

  const { data: reviewsData, isLoading } = useAdminReviews(
    tenantId,
    roomId,
    0,
    50
  );

  const reviews = reviewsData?.content || [];
  const totalReviews = reviews.length;
  const avgRating =
    totalReviews > 0
      ? (reviews.reduce((acc, r) => acc + r.rating, 0) / totalReviews).toFixed(1)
      : '0.0';

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col p-6">
          <DialogHeader className="pb-3 border-b">
            <div className="flex items-start justify-between gap-4">
              <div>
                <DialogTitle className="text-xl font-bold flex items-center gap-2">
                  <Star className="h-5 w-5 text-amber-500 fill-amber-500" />
                  Reviews — {roomName}
                </DialogTitle>
                <DialogDescription className="mt-1 text-xs text-muted-foreground">
                  Guest feedback and ratings specifically for this room.
                </DialogDescription>
              </div>

              {totalReviews > 0 && (
                <div className="flex items-center gap-2 bg-muted/60 px-3 py-1.5 rounded-lg shrink-0">
                  <Star className="h-4 w-4 text-amber-500 fill-amber-500" />
                  <span className="font-bold text-sm text-foreground">{avgRating}</span>
                  <span className="text-xs text-muted-foreground">({totalReviews})</span>
                </div>
              )}
            </div>
          </DialogHeader>

          {/* Body */}
          <div className="flex-1 overflow-y-auto py-4 space-y-4 pr-1">
            {isLoading ? (
              <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
                <Loader2 className="h-7 w-7 animate-spin mb-2 text-primary" />
                <span className="text-sm">Loading room reviews…</span>
              </div>
            ) : reviews.length === 0 ? (
              <div className="text-center py-12 border border-dashed rounded-lg bg-muted/10">
                <Star className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                <p className="text-sm font-medium text-muted-foreground">No reviews yet for this room</p>
                <p className="text-xs text-muted-foreground/80 mt-1">
                  Reviews submitted by guests who completed their stay will show up here.
                </p>
              </div>
            ) : (
              reviews.map((review) => {
                const customerName =
                  [review.customerFirstName, review.customerLastName].filter(Boolean).join(' ').trim() ||
                  'Verified Guest';

                return (
                  <div
                    key={review.id}
                    className="p-4 rounded-lg border bg-card/60 space-y-3 hover:border-border transition-colors"
                  >
                    {/* Header */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <Avatar className="h-8 w-8">
                          <AvatarFallback className="bg-primary/10 text-primary text-xs font-semibold">
                            {review.customerFirstName?.[0]?.toUpperCase() || <User className="h-4 w-4" />}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="text-sm font-medium text-foreground">
                              {customerName}
                            </span>
                            {review.isVerified && (
                              <Badge
                                variant="outline"
                                className="text-[10px] py-0 px-1.5 gap-0.5 border-emerald-500/30 text-emerald-600 dark:text-emerald-400 bg-emerald-500/5"
                              >
                                <ShieldCheck className="h-2.5 w-2.5" />
                                Verified Stay
                              </Badge>
                            )}
                          </div>
                          <span className="text-[11px] text-muted-foreground">
                            {formatDate(review.createdAt)}
                          </span>
                        </div>
                      </div>

                      <StarRating value={review.rating} readonly size="sm" />
                    </div>

                    {/* Review text */}
                    {review.comment ? (
                      <p className="text-xs text-foreground/90 whitespace-pre-line leading-relaxed">
                        {review.comment}
                      </p>
                    ) : (
                      <p className="text-xs text-muted-foreground italic">
                        Rating only — no text review provided.
                      </p>
                    )}

                    {/* Management reply (if any) */}
                    {review.reply && (
                      <div className="rounded-md border-l-2 border-primary bg-muted/40 p-3 space-y-1 text-xs">
                        <div className="flex items-center justify-between text-[11px] text-muted-foreground font-medium">
                          <span className="flex items-center gap-1 text-primary">
                            <Building2 className="h-3 w-3" />
                            Response ({review.repliedByRole ? review.repliedByRole.replace('_', ' ') : 'Management'})
                          </span>
                          {review.repliedAt && <span>{formatDate(review.repliedAt)}</span>}
                        </div>
                        <p className="text-foreground/90 leading-relaxed">{review.reply}</p>
                      </div>
                    )}

                    {/* Actions: Admin can reply/edit reply; Owner has no reply action */}
                    {canReply && (
                      <div className="flex justify-end pt-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-7 text-xs text-muted-foreground hover:text-foreground"
                          onClick={() => setReviewToReply(review)}
                        >
                          <MessageSquare className="h-3.5 w-3.5 mr-1" />
                          {review.reply ? 'Edit Reply' : 'Reply'}
                        </Button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Reply dialog for Admin */}
      {canReply && (
        <ReplyReviewDialog
          review={reviewToReply}
          open={!!reviewToReply}
          onOpenChange={(open) => !open && setReviewToReply(null)}
        />
      )}
    </>
  );
}
