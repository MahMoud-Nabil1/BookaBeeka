import api from '../../../config/api';
import type { NotificationDto } from '../../../types/notification';

export const notificationApi = {
  /**
   * Get notifications for the currently authenticated user
   */
  getMyNotifications: async (tenantId: string): Promise<NotificationDto[]> => {
    const response = await api.get<NotificationDto[]>('/notifications/my-notifications', {
      params: { tenantId },
    });
    return response.data;
  },

  /**
   * Get notifications for a specific customer (staff view)
   */
  getCustomerNotifications: async (
    customerId: string,
    tenantId: string
  ): Promise<NotificationDto[]> => {
    const response = await api.get<NotificationDto[]>(
      `/notifications/customer/${customerId}`,
      {
        params: { tenantId },
      }
    );
    return response.data;
  },

  /**
   * Get a single notification by ID
   */
  getNotification: async (id: string): Promise<NotificationDto> => {
    const response = await api.get<NotificationDto>(`/notifications/${id}`);
    return response.data;
  },
};
