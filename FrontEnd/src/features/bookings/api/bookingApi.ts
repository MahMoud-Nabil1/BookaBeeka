import api from '../../../config/api';
import type {
  CreateBookingRequestDto,
  BookingConfirmationDto,
  BookingDto,
  BookingStatusDto,
  CancellationResultDto,
} from '../../../types/booking';

type BackendCreateBookingRequest = {
  tenantId: string;
  roomId?: string;
  resourceId?: string;
  serviceOfferingId?: string;
  start?: string;
  end?: string;
  checkInDate?: string;
  checkOutDate?: string;
  numberOfRooms?: number;
  specialRequests?: string;
};

function mapToBackendBookingRequest(req: CreateBookingRequestDto): BackendCreateBookingRequest {
  const checkIn = req.start ? req.start.split('T')[0] : undefined;
  const checkOut = req.end ? req.end.split('T')[0] : undefined;
  return {
    tenantId: req.tenantId,
    roomId: req.roomId,
    resourceId: req.roomId,
    serviceOfferingId: req.roomTypeId || undefined,
    start: req.start,
    end: req.end,
    checkInDate: checkIn,
    checkOutDate: checkOut,
    numberOfRooms: 1,
  };
}

type BackendBookingDto = {
  bookingId: string;
  tenantId: string;
  customerId: string;
  roomId?: string;
  resourceId?: string;
  serviceOfferingId?: string;
  startTime: string;
  endTime: string;
  status: string;
  totalAmount: number;
  currency: string;
  cancellationReason: string | null;
  version: number;
  createdAt: string;
};

function mapToFrontendBookingDto(backend: BackendBookingDto): BookingDto {
  return {
    bookingId: backend.bookingId,
    tenantId: backend.tenantId,
    customerId: backend.customerId,
    roomId: backend.roomId || backend.resourceId || '',
    roomTypeId: backend.serviceOfferingId || '',
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
    tenantId?: string | null,
    reason?: string
  ): Promise<CancellationResultDto> => {
    const response = await api.post(`/api/bookings/${bookingId}/cancel`, null, {
      params: { ...(tenantId ? { tenantId } : {}), ...(reason ? { reason } : {}) },
    });
    return response.data;
  },

  // GET /api/bookings/{bookingId}/status?tenantId= (tenantId optional)
  getBookingStatus: async (
    bookingId: string,
    tenantId?: string | null
  ): Promise<BookingStatusDto> => {
    const response = await api.get(`/api/bookings/${bookingId}/status`, {
      params: tenantId ? { tenantId } : {},
    });
    return response.data;
  },

  // GET /api/bookings/mine?tenantId= (tenantId optional)
  // Returns all bookings for the authenticated customer (customerId from JWT)
  // When tenantId is omitted, returns bookings across all tenants
  getMyBookings: async (tenantId?: string | null): Promise<BookingDto[]> => {
    const response = await api.get<BackendBookingDto[]>('/api/bookings/mine', {
      params: tenantId ? { tenantId } : {},
    });
    return response.data.map(mapToFrontendBookingDto);
  },
};
