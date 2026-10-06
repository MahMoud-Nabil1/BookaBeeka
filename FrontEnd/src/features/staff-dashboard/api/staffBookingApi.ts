import api from '../../../config/api';
import type { BookingDto, CancellationResultDto } from '../../../types/booking';

export const staffBookingApi = {
  // GET /api/bookings/mine?tenantId=
  // For staff the backend still uses the authenticated user's tenantId from JWT,
  // but we pass tenantId as a query param to satisfy the controller signature.
  // NOTE: The backend GET /api/bookings/mine returns bookings scoped to the
  // authenticated customer. For staff viewing ALL tenant bookings, use the
  // confirm/cancel flows which are keyed by bookingId + tenantId.
  getTenantBookings: async (tenantId: string): Promise<BookingDto[]> => {
    const response = await api.get('/api/bookings/mine', {
      params: { tenantId },
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

  // POST /api/bookings/{bookingId}/complete
  // Admin/Owner only — marks booking as COMPLETED after checkout date
  completeBooking: async (bookingId: string): Promise<{ message: string; bookingId: string }> => {
    const response = await api.post(`/api/bookings/${bookingId}/complete`);
    return response.data;
  },
};
