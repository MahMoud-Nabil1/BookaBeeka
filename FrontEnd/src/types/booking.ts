export type BookingStatus =
  | 'PENDING_PAYMENT'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'EXPIRED';

// POST /api/bookings
// Frontend uses hotel terms (roomId, roomTypeId), backend API uses (resourceId, serviceOfferingId)
export interface CreateBookingRequestDto {
  tenantId: string;
  roomId: string;              // Maps to backend: resourceId
  roomTypeId: string;          // Maps to backend: serviceOfferingId
  start: string;               // OffsetDateTime ISO string (check-in)
  end: string;                 // OffsetDateTime ISO string (check-out)
}

// 201 response from POST /api/bookings
export interface BookingConfirmationDto {
  bookingId: string;
  status: BookingStatus;
  lockId: string;
  createdAt: string;
}

// Full booking read — GET /api/bookings/mine, GET /api/bookings/{id}/status
export interface BookingDto {
  bookingId: string;
  tenantId: string;
  customerId: string;
  roomId: string;              // Maps to backend: resourceId
  roomTypeId: string;          // Maps to backend: serviceOfferingId
  startTime: string;           // Check-in time
  endTime: string;             // Check-out time
  status: BookingStatus;
  totalAmount: number;
  currency: string;
  cancellationReason: string | null;
  version: number;
  createdAt: string;
}

// GET /api/bookings/{bookingId}/status
export interface BookingStatusDto {
  bookingId: string;
  status: BookingStatus;
  version: number;
}

// POST /api/bookings/{bookingId}/cancel
export interface CancellationResultDto {
  bookingId: string;
  refundAmount: number;
  refundPercentage: number;
  newStatus: BookingStatus;
}
