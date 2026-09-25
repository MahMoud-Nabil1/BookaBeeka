import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Loader2, AlertCircle, CheckCircle2, ShieldCheck, PenLine } from 'lucide-react';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import StarRating from './StarRating';
import { useCreateReview } from '../hooks/useReviews';
import { bookingApi } from '../../bookings/api/bookingApi';
import { useAppSelector } from '../../../redux/hooks';
import {
  selectIsAuthenticated,
  selectUserType,
} from '../../../redux/selectors/authSelectors';

interface AddReviewModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  serviceId: string;
  tenantId: string;
  roomName?: string;
  preselectedBookingId?: string;
}

export default function AddReviewModal({
  open,
  onOpenChange,
  serviceId,
  tenantId,
  roomName,
  preselectedBookingId,
}: AddReviewModalProps) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const userType = useAppSelector(selectUserType);
  const isCustomer = isAuthenticated && userType === 'CUSTOMER';

  const [rating, setRating] = useState<number>(5);
  const [comment, setComment] = useState<string>('');
  const [selectedBookingId, setSelectedBookingId] = useState<string>(
    preselectedBookingId || ''
  );

  const { mutate: submitReview, isPending: isSubmitting } = useCreateReview();

  // Fetch customer bookings to locate eligible completed stays
  const { data: myBookings, isLoading: loadingBookings } = useQuery({
    queryKey: ['my-bookings', tenantId],
    queryFn: () => bookingApi.getMyBookings(tenantId),
    enabled: isCustomer && open,
  });

  // Filter completed bookings
  const completedBookings =
    myBookings?.filter((b) => b.status === 'COMPLETED') || [];

  // Match bookings specifically for this room/service if possible, or any completed booking for the tenant
  const eligibleRoomBookings = completedBookings.filter(
    (b) => b.roomId === serviceId || !b.roomId
  );
  const eligibleBookings =
    eligibleRoomBookings.length > 0 ? eligibleRoomBookings : completedBookings;

  useEffect(() => {
    if (preselectedBookingId) {
      setSelectedBookingId(preselectedBookingId);
    } else if (eligibleBookings.length > 0 && !selectedBookingId) {
      setSelectedBookingId(eligibleBookings[0].bookingId);
    }
  }, [eligibleBookings, preselectedBookingId, selectedBookingId]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBookingId) return;

    submitReview(
      {
        tenantId,
        serviceId,
        bookingId: selectedBookingId,
        rating,
        comment: comment.trim() || undefined,
      },
      {
        onSuccess: () => {
          onOpenChange(false);
          setComment('');
          setRating(5);
        },
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl font-bold">
            <PenLine className="h-5 w-5 text-primary" />
            Write a Review
          </DialogTitle>
          <DialogDescription>
            {roomName ? `Share your experience for ${roomName}` : 'Share your verified guest experience.'}
          </DialogDescription>
        </DialogHeader>

        {/* Not Logged In */}
        {!isCustomer ? (
          <div className="py-6 text-center space-y-4">
            <AlertCircle className="h-10 w-10 text-amber-500 mx-auto" />
            <div className="space-y-1">
              <h3 className="font-semibold text-base">Customer Account Required</h3>
              <p className="text-sm text-muted-foreground">
                You must be logged in as a verified guest to write a review.
              </p>
            </div>
            <div className="pt-2 flex justify-center gap-3">
              <Button asChild variant="default">
                <Link to="/login/customer">Log In as Customer</Link>
              </Button>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
            </div>
          </div>
        ) : loadingBookings ? (
          /* Loading customer bookings */
          <div className="py-12 flex flex-col items-center justify-center text-muted-foreground gap-2">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
            <span className="text-sm">Checking booking history...</span>
          </div>
        ) : eligibleBookings.length === 0 ? (
          /* No completed bookings eligible */
          <div className="py-6 space-y-4 text-center">
            <div className="p-3 bg-muted/60 rounded-full w-fit mx-auto text-primary">
              <ShieldCheck className="h-8 w-8" />
            </div>
            <div className="space-y-1">
              <h3 className="font-semibold text-base">Verified Stays Only</h3>
              <p className="text-sm text-muted-foreground max-w-sm mx-auto leading-relaxed">
                Only guests who have completed a stay can review this accommodation. If you have an active reservation, you can review as soon as your stay completes!
              </p>
            </div>
            <div className="pt-2 flex justify-center gap-3">
              <Button asChild variant="outline">
                <Link to="/portal/bookings">View My Bookings</Link>
              </Button>
              <Button variant="default" onClick={() => onOpenChange(false)}>
                Got it
              </Button>
            </div>
          </div>
        ) : (
          /* Review Form */
          <form onSubmit={handleSubmit} className="space-y-5 py-2">
            {/* Eligible booking selection if multiple */}
            {eligibleBookings.length > 1 && (
              <div className="space-y-1.5">
                <Label htmlFor="booking-select" className="text-xs font-semibold">
                  Select Completed Stay
                </Label>
                <Select
                  value={selectedBookingId}
                  onValueChange={setSelectedBookingId}
                >
                  <SelectTrigger id="booking-select" className="w-full">
                    <SelectValue placeholder="Choose your stay" />
                  </SelectTrigger>
                  <SelectContent>
                    {eligibleBookings.map((b) => (
                      <SelectItem key={b.bookingId} value={b.bookingId}>
                        Booking Ref: {b.bookingId.substring(0, 8)}... (Completed)
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Rating Stars Picker */}
            <div className="space-y-2">
              <Label className="text-xs font-semibold">Overall Rating</Label>
              <div className="flex items-center gap-3 p-3 bg-muted/30 border border-border rounded-lg">
                <StarRating
                  value={rating}
                  onChange={setRating}
                  size="lg"
                  className="gap-2"
                />
                <span className="text-sm font-semibold text-foreground">
                  {rating === 5 && 'Outstanding'}
                  {rating === 4 && 'Very Good'}
                  {rating === 3 && 'Average'}
                  {rating === 2 && 'Poor'}
                  {rating === 1 && 'Terrible'}
                </span>
              </div>
            </div>

            {/* Review Comment */}
            <div className="space-y-1.5">
              <div className="flex justify-between items-center">
                <Label htmlFor="review-comment" className="text-xs font-semibold">
                  Written Feedback (Optional)
                </Label>
                <span className="text-[11px] text-muted-foreground">
                  {comment.length}/2000
                </span>
              </div>
              <Textarea
                id="review-comment"
                placeholder="What did you enjoy most about your stay? How was the service, cleanliness, and comfort?"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                maxLength={2000}
                rows={4}
                className="resize-none text-sm"
              />
            </div>

            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
              <span>Your review will appear as a Verified Guest Stay.</span>
            </div>

            <DialogFooter className="pt-2 gap-2 sm:gap-0">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || !selectedBookingId || rating < 1}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Submitting...
                  </>
                ) : (
                  'Submit Review'
                )}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
