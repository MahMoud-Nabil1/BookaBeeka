export interface SlotDto {
  start: string;
  end: string;
}

export interface SlotLockDto {
  lockId: string;
  start: string;
  end: string;
  expiresAt: string;
  status: string;
}

export interface ScheduleRuleDto {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
}

export interface ExceptionDto {
  exceptionDate: string;
  isAvailable: boolean;
  startTime: string;
  endTime: string;
  reason: string;
}

export interface RoomBlockResponse {
  id: string;
  tenantId: string;
  roomId: string;
  startDate: string; // ISO date
  endDate: string; // ISO date
  reason: string | null;
  createdAt: string;
}

export interface CreateRoomBlockRequest {
  roomId: string;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface UpdateRoomBlockRequest {
  startDate?: string;
  endDate?: string;
  reason?: string;
}
