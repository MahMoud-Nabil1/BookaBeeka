export interface ReviewDto {
  id: string;
  tenantId: string;
  bookingId: string;
  customerId: string;
  serviceId: string;
  staffId?: string | null;
  rating: number; // 1 to 5
  comment?: string | null;
  isVerified: boolean;
  reply?: string | null;
  repliedBy?: string | null;
  repliedByRole?: string | null;
  repliedAt?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface CreateReviewRequest {
  tenantId: string;
  bookingId: string;
  serviceId: string;
  rating: number;
  comment?: string;
}

export interface ReplyToReviewRequest {
  reply: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
