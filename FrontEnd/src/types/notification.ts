export type NotificationType =
  | 'BOOKING_CONFIRMED'
  | 'BOOKING_CANCELLED'
  | 'PAYMENT_RECEIVED'
  | 'BOOKING_REMINDER'
  | 'REVIEW_REPLY';

export interface NotificationDto {
  id: string;
  tenantId: string;
  customerId: string;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}
