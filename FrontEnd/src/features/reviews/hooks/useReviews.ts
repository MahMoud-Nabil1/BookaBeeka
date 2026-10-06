import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { reviewApi } from '../api/reviewApi';
import type { CreateReviewRequest } from '../../../types/review';

/**
 * Fetch reviews for a specific room / service
 */
export function useServiceReviews(
  serviceId?: string,
  tenantId?: string,
  page = 0,
  size = 10
) {
  return useQuery({
    queryKey: ['reviews', serviceId, tenantId, page, size],
    queryFn: () => reviewApi.getReviewsForService(serviceId!, tenantId!, page, size),
    enabled: Boolean(serviceId && tenantId),
    staleTime: 1000 * 60 * 3, // 3 minutes
  });
}

/**
 * Fetch logged-in customer's reviews
 */
export function useMyReviews(page = 0, size = 10) {
  return useQuery({
    queryKey: ['my-reviews', page, size],
    queryFn: () => reviewApi.getMyReviews(page, size),
    staleTime: 1000 * 60 * 2,
  });
}

/**
 * Fetch reviews for a specific room (resource) — preferred for the room detail page
 */
export function useRoomReviews(
  roomId?: string,
  tenantId?: string,
  page = 0,
  size = 10
) {
  return useQuery({
    queryKey: ['room-reviews', roomId, tenantId, page, size],
    queryFn: () => reviewApi.getReviewsForRoom(roomId!, tenantId!, page, size),
    enabled: Boolean(roomId && tenantId),
    staleTime: 1000 * 60 * 3, // 3 minutes
  });
}

/**
 * Hook to submit a new review on a completed booking
 */
export function useCreateReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateReviewRequest) => reviewApi.createReview(request),
    onSuccess: (_data, _variables) => {
      toast.success('Review submitted successfully!', {
        description: 'Thank you for sharing your feedback.',
      });
      // Invalidate both service-based and room-based queries so the new review appears immediately
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      queryClient.invalidateQueries({ queryKey: ['room-reviews'] });
      queryClient.invalidateQueries({ queryKey: ['my-reviews'] });
    },
    onError: (error: any) => {
      const responseData = error.response?.data;
      const message =
        (responseData?.message && responseData.message !== 'No further cause')
          ? responseData.message
          : (responseData?.error || 'Failed to submit review. Only completed stays can be reviewed once.');
      toast.error('Unable to submit review', {
        description: message,
      });
    },
  });
}

/**
 * Hook for staff to reply to a review
 */
export function useReplyToReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      reviewId,
      reply,
      tenantId,
    }: {
      reviewId: string;
      reply: string;
      tenantId?: string | null;
    }) => reviewApi.replyToReview(reviewId, reply, tenantId),
    onSuccess: () => {
      toast.success('Reply posted successfully');
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      queryClient.invalidateQueries({ queryKey: ['admin-reviews'] });
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message || 'Failed to post reply to review.';
      toast.error('Reply failed', { description: message });
    },
  });
}
