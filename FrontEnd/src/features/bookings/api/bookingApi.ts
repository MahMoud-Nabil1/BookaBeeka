import api from '../../../config/api';
import type {
  CreateBookingRequestDto,
  BookingConfirmationDto,
  BookingDto,
  BookingStatusDto,
  CancellationResultDto,
} from '../../../types/booking';

// ── API Adapters ──────────────────────────────────────────────────────────────
// Map between frontend hotel terminology and backend generic API

type BackendCreateBookingRequest = {
  tenantId: string;
  resourceId: string;
  serviceOfferingId: string;
  start: string;
  end: string;
};

type BackendBookingDto = {
  bookingId: string;
  tenantId: string;
  customerId: string;
  resourceId: string;
  serviceOfferingId: string;
  startTime: string;
  endTime: string;
  status: string;
  totalAmount: number;
  currency: string;
  cancellationReason: string | null;
  version: number;
  createdAt: string;
};

function mapToBackendBookingRequest(req: CreateBookingRequestDto): BackendCreateBookingRequest {
  return {
    tenantId: req.tenantId,
    resourceId: req.roomId,
    serviceOfferingId: req.roomTypeId,
    start: req.start,
    end: req.end,
  };
}

function mapToFrontendBookingDto(backend: BackendBookingDto): BookingDto {
  return {
    bookingId: backend.bookingId,
    tenantId: backend.tenantId,
    customerId: backend.customerId,
    roomId: backend.resourceId,
    roomTypeId: backend.serviceOfferingId,
    startTime: backend.startTime,
    endTime: backend.endTime,
    status: backend.status as BookingDto['status'],
    totalAmount: backend.totalAmount,
    currency: backend.currency,
    cancellationReason: backend.cancellationReason,
    version: backend.version,
    createdAt: backend.createdAt,
  };
}

export const bookingApi = {
  // POST /api/bookings  [Header: Idempotency-Key]
  // customerId is extracted from JWT on the server — not sent by client
  createBooking: async (
    req: CreateBookingRequestDto,
    idempotencyKey?: string
  ): Promise<BookingConfirmationDto> => {
    const backendReq = mapToBackendBookingRequest(req);
    const response = await api.post('/api/bookings', backendReq, {
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
    });
    return response.data;
  },

  // POST /api/bookings/{bookingId}/confirm?tenantId=
  confirmBooking: async (
    bookingId: string,
    tenantId: string
  ): Promise<{ message: string; bookingId: string }> => {
    const response = await api.post(`/api/bookings/${bookingId}/confirm`, null, {
      params: { tenantId },
    });
    return response.data;
  },

  // POST /api/bookings/{bookingId}/cancel?tenantId=&reason=
  cancelBooking: async (
    bookingId: string,
    tenantId: string,
    reason?: string
  ): Promise<CancellationResultDto> => {
    const response = await api.post(`/api/bookings/${bookingId}/cancel`, null, {
      params: { tenantId, ...(reason ? { reason } : {}) },
    });
    return response.data;
  },

  // GET /api/bookings/{bookingId}/status?tenantId=
  getBookingStatus: async (
    bookingId: string,
    tenantId: string
  ): Promise<BookingStatusDto> => {
    const response = await api.get(`/api/bookings/${bookingId}/status`, {
      params: { tenantId },
    });
    return response.data;
  },

  // GET /api/bookings/mine?tenantId=
  // Returns all bookings for the authenticated customer (customerId from JWT)
  getMyBookings: async (tenantId: string): Promise<BookingDto[]> => {
    const response = await api.get<BackendBookingDto[]>('/api/bookings/mine', {
      params: { tenantId },
    });
    return response.data.map(mapToFrontendBookingDto);
  },
};
