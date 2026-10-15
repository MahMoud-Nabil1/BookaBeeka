import { useState, useMemo } from 'react';
import {
  Star,
  MessageSquare,
  Building2,
  Filter,
  Loader2,
  ShieldCheck,
  User,
  DoorOpen,
  CheckCircle2,
  Clock,
} from 'lucide-react';
import PageLayout from '../../../../components/layout/PageLayout';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import StarRating from '../../../reviews/components/StarRating';
import { useAdminReviews } from '../../../reviews/hooks/useReviews';
import { useRooms } from '../../../catalog/hooks/useInventory';
import { ReplyReviewDialog } from '../components/ReplyReviewDialog';
import { useAppSelector } from '../../../../redux/hooks';
import { selectRole, selectTenantId } from '../../../../redux/selectors/authSelectors';
import type { ReviewDto } from '../../../../types/review';

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

export default function HotelReviewsPage() {
  const tenantId = useAppSelector(selectTenantId);
  const role = useAppSelector(selectRole);
  // ADMIN and SUPER_ADMIN can reply; OWNER cannot reply.
  const canReply = role === 'ADMIN' || role === 'SUPER_ADMIN';

  // Filters state
  const [selectedRoomId, setSelectedRoomId] = useState<string>('ALL');
  const [selectedRating, setSelectedRating] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);

  // Review reply state
  const [reviewToReply, setReviewToReply] = useState<ReviewDto | null>(null);

  // Fetch rooms to display in room selector and map IDs to names
  const { data: rooms = [] } = useRooms(tenantId ?? undefined);
  const roomMap = useMemo(() => {
    const map = new Map<string, string>();
    rooms.forEach((r) => map.set(r.id, r.name));
    return map;
  }, [rooms]);

  // Fetch reviews (scoped to tenant and optionally room)
  const effectiveRoomId = selectedRoomId !== 'ALL' ? selectedRoomId : null;
  const { data: pagedReviews, isLoading } = useAdminReviews(
    tenantId,
    effectiveRoomId,
    page,
    50
  );

  const reviews = pagedReviews?.content || [];

  // Filter client-side by rating and reply status if set
  const filteredReviews = useMemo(() => {
    return reviews.filter((r) => {
      if (selectedRating !== 'ALL' && r.rating !== Number(selectedRating)) {
        return false;
      }
      if (selectedStatus === 'REPLIED' && !r.reply) {
        return false;
      }
      if (selectedStatus === 'NEEDS_REPLY' && r.reply) {
        return false;
      }
      return true;
    });
  }, [reviews, selectedRating, selectedStatus]);

  // Overall Statistics
  const totalReviews = reviews.length;
  const avgRating =
    totalReviews > 0
      ? (reviews.reduce((acc, r) => acc + r.rating, 0) / totalReviews).toFixed(1)
      : '0.0';
  const repliedCount = reviews.filter((r) => r.reply).length;
  const pendingCount = totalReviews - repliedCount;

  return (
    <PageLayout
      title="Guest Reviews"
      description={
        canReply
          ? 'Monitor customer ratings, inspect feedback on each room, and reply to guests.'
          : 'Monitor customer ratings and feedback across your hotel rooms.'
      }
    >
      <div className="space-y-6">
        {/* Metric Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Average Rating</CardTitle>
              <div className="p-2 rounded-lg bg-amber-500/10 text-amber-500">
                <Star className="h-4 w-4 fill-amber-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold flex items-baseline gap-2">
                <span>{avgRating}</span>
                <span className="text-xs text-muted-foreground font-normal">/ 5.0</span>
              </div>
              <p className="text-xs text-muted-foreground mt-1">Based on {totalReviews} reviews</p>
            </CardContent>
          </Card>

          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Total Reviews</CardTitle>
              <div className="p-2 rounded-lg bg-blue-500/10 text-blue-500">
                <MessageSquare className="h-4 w-4" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{totalReviews}</div>
              <p className="text-xs text-muted-foreground mt-1">Across all hotel rooms</p>
            </CardContent>
          </Card>

          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Answered</CardTitle>
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-500">
                <CheckCircle2 className="h-4 w-4" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{repliedCount}</div>
              <p className="text-xs text-muted-foreground mt-1">Responses published</p>
            </CardContent>
          </Card>

          <Card className="border-border">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">Awaiting Reply</CardTitle>
              <div className="p-2 rounded-lg bg-yellow-500/10 text-yellow-500">
                <Clock className="h-4 w-4" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{pendingCount}</div>
              <p className="text-xs text-muted-foreground mt-1">
                {canReply ? 'Reviews needing response' : 'Unanswered reviews'}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Filter Controls Bar */}
        <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-lg border bg-card/60">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground mr-1">
              <Filter className="h-3.5 w-3.5" />
              <span className="font-medium">Filter By:</span>
            </div>

            {/* Room Filter Selector */}
            <Select
              value={selectedRoomId}
              onValueChange={(val) => {
                setSelectedRoomId(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[200px] h-9 text-xs">
                <DoorOpen className="h-3.5 w-3.5 mr-1.5 text-muted-foreground" />
                <SelectValue placeholder="All Rooms" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Rooms</SelectItem>
                {rooms.map((room) => (
                  <SelectItem key={room.id} value={room.id}>
                    {room.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Rating Filter Selector */}
            <Select
              value={selectedRating}
              onValueChange={(val) => {
                setSelectedRating(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[140px] h-9 text-xs">
                <Star className="h-3.5 w-3.5 mr-1.5 text-muted-foreground" />
                <SelectValue placeholder="Rating" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Ratings</SelectItem>
                <SelectItem value="5">5 Stars</SelectItem>
                <SelectItem value="4">4 Stars</SelectItem>
                <SelectItem value="3">3 Stars</SelectItem>
                <SelectItem value="2">2 Stars</SelectItem>
                <SelectItem value="1">1 Star</SelectItem>
              </SelectContent>
            </Select>

            {/* Reply Status Filter */}
            <Select
              value={selectedStatus}
              onValueChange={(val) => {
                setSelectedStatus(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[150px] h-9 text-xs">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                <SelectItem value="REPLIED">Replied</SelectItem>
                <SelectItem value="NEEDS_REPLY">Needs Reply</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="text-xs text-muted-foreground">
            Showing <span className="font-semibold text-foreground">{filteredReviews.length}</span>{' '}
            {filteredReviews.length === 1 ? 'review' : 'reviews'}
          </div>
        </div>

        {/* Reviews List */}
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
            <Loader2 className="h-8 w-8 animate-spin mb-3 text-primary" />
            <span className="text-sm">Loading hotel reviews…</span>
          </div>
        ) : filteredReviews.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 border border-dashed rounded-lg bg-card/40">
            <Star className="h-10 w-10 text-muted-foreground/30 mb-3" />
            <p className="text-base font-semibold text-foreground">No reviews found</p>
            <p className="text-xs text-muted-foreground mt-1 max-w-sm text-center">
              {selectedRoomId !== 'ALL' || selectedRating !== 'ALL' || selectedStatus !== 'ALL'
                ? 'Try adjusting your filters to see more results.'
                : 'Customer reviews will appear here once guests complete their reservations and leave feedback.'}
            </p>
            {(selectedRoomId !== 'ALL' || selectedRating !== 'ALL' || selectedStatus !== 'ALL') && (
              <Button
                variant="outline"
                size="sm"
                className="mt-4 text-xs"
                onClick={() => {
                  setSelectedRoomId('ALL');
                  setSelectedRating('ALL');
                  setSelectedStatus('ALL');
                }}
              >
                Reset Filters
              </Button>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {filteredReviews.map((review) => {
              const customerName =
                [review.customerFirstName, review.customerLastName].filter(Boolean).join(' ').trim() ||
                'Verified Guest';
              const roomName = review.roomId ? roomMap.get(review.roomId) || 'Hotel Room' : 'Hotel Stay';

              return (
                <Card
                  key={review.id}
                  className="border-border/80 shadow-none hover:border-border transition-colors bg-card"
                >
                  <CardContent className="p-5 space-y-4">
                    {/* Header: Reviewer info, Room badge, Rating */}
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <Avatar className="h-10 w-10 border">
                          <AvatarFallback className="bg-primary/10 text-primary font-semibold text-sm">
                            {review.customerFirstName?.[0]?.toUpperCase() || (
                              <User className="h-5 w-5 text-primary" />
                            )}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-sm text-foreground">
                              {customerName}
                            </span>
                            {review.isVerified && (
                              <Badge
                                variant="outline"
                                className="gap-1 border-emerald-500/30 text-emerald-600 dark:text-emerald-400 text-[11px] font-medium py-0 px-1.5 bg-emerald-500/5"
                              >
                                <ShieldCheck className="h-3 w-3" />
                                Verified Stay
                              </Badge>
                            )}
                          </div>
                          <div className="flex items-center gap-2 mt-0.5 text-xs text-muted-foreground">
                            <span>{formatDate(review.createdAt)}</span>
                            <span>•</span>
                            <Badge variant="secondary" className="text-[11px] font-normal py-0 px-2 gap-1">
                              <DoorOpen className="h-3 w-3" />
                              {roomName}
                            </Badge>
                          </div>
                        </div>
                      </div>

                      <StarRating value={review.rating} readonly size="sm" />
                    </div>

                    {/* Review text comment */}
                    {review.comment ? (
                      <p className="text-sm text-foreground/90 leading-relaxed whitespace-pre-line pl-1">
                        {review.comment}
                      </p>
                    ) : (
                      <p className="text-xs italic text-muted-foreground pl-1">
                        Guest left a rating with no written comment.
                      </p>
                    )}

                    {/* Management Reply Section */}
                    {review.reply && (
                      <div className="rounded-lg border-l-4 border-primary bg-muted/40 p-4 space-y-2">
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                            <Building2 className="h-3.5 w-3.5 text-primary" />
                            <span>
                              Response from{' '}
                              {review.repliedByRole ? review.repliedByRole.replace('_', ' ') : 'Management'}
                            </span>
                          </div>
                          {review.repliedAt && (
                            <span className="text-[11px] text-muted-foreground">
                              {formatDate(review.repliedAt)}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-foreground/80 leading-relaxed">{review.reply}</p>
                      </div>
                    )}

                    {/* Action Bar: Admin can Reply/Edit Reply, Owner has NO reply action */}
                    {canReply && (
                      <div className="flex justify-end pt-1 border-t border-border/50">
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-xs h-8 gap-1.5"
                          onClick={() => setReviewToReply(review)}
                        >
                          <MessageSquare className="h-3.5 w-3.5" />
                          {review.reply ? 'Edit Official Reply' : 'Reply to Guest'}
                        </Button>
                      </div>
                    )}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </div>

      {/* Reply dialog for Admin */}
      {canReply && (
        <ReplyReviewDialog
          review={reviewToReply}
          open={!!reviewToReply}
          onOpenChange={(open) => !open && setReviewToReply(null)}
        />
      )}
    </PageLayout>
  );
}
