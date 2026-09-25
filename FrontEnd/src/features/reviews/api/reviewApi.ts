import api from '../../../config/api';
import type {
  ReviewDto,
  CreateReviewRequest,
  ReplyToReviewRequest,
  PageResponse,
} from '../../../types/review';

export const reviewApi = {
  /**
   * Fetch reviews for a specific room / service (product)
   */
  getReviewsForService: async (
    serviceId: string,
    tenantId: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<ReviewDto>> => {
    const response = await api.get<PageResponse<ReviewDto>>(`/api/reviews/service/${serviceId}`, {
      params: { tenantId, page, size },
    });
    return response.data;
  },

  /**
   * Fetch reviews created by the authenticated customer
   */
  getMyReviews: async (page = 0, size = 20): Promise<PageResponse<ReviewDto>> => {
    const response = await api.get<PageResponse<ReviewDto>>('/api/reviews/mine', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Customer submits a review on a completed booking
   */
  createReview: async (request: CreateReviewRequest): Promise<ReviewDto> => {
    const response = await api.post<ReviewDto>('/api/reviews', request);
    return response.data;
  },

  /**
   * Staff/Admin: Fetch all reviews for their tenant
   */
  getAdminReviews: async (
    tenantId?: string | null,
    page = 0,
    size = 20
  ): Promise<PageResponse<ReviewDto>> => {
    const response = await api.get<PageResponse<ReviewDto>>('/api/admin/reviews', {
      params: {
        ...(tenantId ? { tenantId } : {}),
        page,
        size,
      },
    });
    return response.data;
  },

  /**
   * Staff/Admin: Reply to a customer review
   */
  replyToReview: async (
    reviewId: string,
    reply: string,
    tenantId?: string | null
  ): Promise<ReviewDto> => {
    const payload: ReplyToReviewRequest = { reply };
    const response = await api.post<ReviewDto>(
      `/api/admin/reviews/${reviewId}/reply`,
      payload,
      {
        params: tenantId ? { tenantId } : {},
      }
    );
    return response.data;
  },
};
