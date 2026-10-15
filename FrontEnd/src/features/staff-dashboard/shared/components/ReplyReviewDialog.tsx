import { useState, useEffect } from 'react';
import { Loader2, MessageSquare, Building2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import StarRating from '../../../reviews/components/StarRating';
import { useReplyToReview } from '../../../reviews/hooks/useReviews';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';
import type { ReviewDto } from '../../../../types/review';

interface ReplyReviewDialogProps {
  review: ReviewDto | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ReplyReviewDialog({
  review,
  open,
  onOpenChange,
}: ReplyReviewDialogProps) {
  const tenantId = useAppSelector(selectTenantId);
  const [replyText, setReplyText] = useState('');
  const { mutate: postReply, isPending } = useReplyToReview();

  useEffect(() => {
    if (review) {
      setReplyText(review.reply || '');
    } else {
      setReplyText('');
    }
  }, [review, open]);

  if (!review) return null;

  const customerName =
    [review.customerFirstName, review.customerLastName].filter(Boolean).join(' ').trim() ||
    'Verified Guest';

  const isEditing = Boolean(review.reply);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!replyText.trim()) return;

    postReply(
      {
        reviewId: review.id,
        reply: replyText.trim(),
        tenantId: review.tenantId || tenantId,
      },
      {
        onSuccess: () => {
          onOpenChange(false);
        },
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-lg font-semibold">
            <Building2 className="h-5 w-5 text-primary" />
            {isEditing ? 'Edit Official Reply' : 'Reply to Customer Review'}
          </DialogTitle>
          <DialogDescription>
            Your response will be visible publicly to all potential guests.
          </DialogDescription>
        </DialogHeader>

        {/* Customer Review Summary Box */}
        <div className="rounded-lg border bg-muted/40 p-3.5 space-y-2 text-sm">
          <div className="flex items-center justify-between">
            <span className="font-medium text-foreground">{customerName}</span>
            <StarRating value={review.rating} readonly size="sm" />
          </div>
          {review.comment ? (
            <p className="text-xs text-muted-foreground italic line-clamp-3">
              "{review.comment}"
            </p>
          ) : (
            <p className="text-xs text-muted-foreground italic">No written comment</p>
          )}
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="reply-text" className="text-sm font-medium">
              Management Response
            </Label>
            <Textarea
              id="reply-text"
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              placeholder="Thank the guest for their stay, address any feedback, and invite them back..."
              rows={4}
              maxLength={1500}
              className="resize-none"
              required
            />
            <div className="flex justify-end text-[11px] text-muted-foreground">
              {replyText.length}/1500 characters
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isPending || !replyText.trim()}>
              {isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Posting Reply…
                </>
              ) : (
                <>
                  <MessageSquare className="mr-2 h-4 w-4" />
                  {isEditing ? 'Update Reply' : 'Post Reply'}
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
