export interface ReviewDto {
  id: string;
  tenantId: string;
  bookingId: string;
  customerId: string;
  customerFirstName?: string | null;
  customerLastName?: string | null;
  serviceId?: string | null;
  roomId?: string | null;
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
  // serviceId is intentionally absent — the backend derives it from the booking
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
