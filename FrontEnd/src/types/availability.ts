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
