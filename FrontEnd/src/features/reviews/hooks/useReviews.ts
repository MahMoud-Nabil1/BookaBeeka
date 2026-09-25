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
 * Hook to submit a new review on a completed booking
 */
export function useCreateReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateReviewRequest) => reviewApi.createReview(request),
    onSuccess: (_, variables) => {
      toast.success('Review submitted successfully!', {
        description: 'Thank you for sharing your feedback.',
      });
      // Invalidate relevant queries so UI refreshes immediately
      queryClient.invalidateQueries({ queryKey: ['reviews', variables.serviceId] });
      queryClient.invalidateQueries({ queryKey: ['my-reviews'] });
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        'Failed to submit review. Only completed stays can be reviewed once.';
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
