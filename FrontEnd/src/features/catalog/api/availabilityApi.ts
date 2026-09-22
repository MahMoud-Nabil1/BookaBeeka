import api from '../../../config/api';
import type { SlotDto, ScheduleRuleDto, ExceptionDto } from '../../../types/availability';

export const availabilityApi = {
  // Public – GET /api/availability/slots?tenantId=&resourceId=&date=YYYY-MM-DD
  getSlots: async (tenantId: string, resourceId: string, date: string): Promise<SlotDto[]> => {
    const response = await api.get('/api/availability/slots', {
      params: { tenantId, resourceId, date },
    });
    return response.data;
  },

  // ── Schedule rules ────────────────────────────────────────────────────────

  // POST /api/availability/schedule-rules?tenantId=&resourceId=
  createScheduleRule: async (
    tenantId: string,
    resourceId: string,
    rule: ScheduleRuleDto
  ) => {
    const response = await api.post('/api/availability/schedule-rules', rule, {
      params: { tenantId, resourceId },
    });
    return response.data;
  },

  // GET /api/availability/schedule-rules?resourceId=
  getScheduleRules: async (resourceId: string) => {
    const response = await api.get('/api/availability/schedule-rules', {
      params: { resourceId },
    });
    return response.data;
  },

  // PUT /api/availability/schedule-rules/{ruleId}?tenantId=
  updateScheduleRule: async (ruleId: string, tenantId: string, rule: ScheduleRuleDto) => {
    const response = await api.put(`/api/availability/schedule-rules/${ruleId}`, rule, {
      params: { tenantId },
    });
    return response.data;
  },

  // DELETE /api/availability/schedule-rules/{ruleId}?tenantId=
  deleteScheduleRule: async (ruleId: string, tenantId: string) => {
    await api.delete(`/api/availability/schedule-rules/${ruleId}`, {
      params: { tenantId },
    });
  },

  // ── Exceptions ────────────────────────────────────────────────────────────

  // POST /api/availability/exceptions?tenantId=&resourceId=
  createException: async (tenantId: string, resourceId: string, exc: ExceptionDto) => {
    const response = await api.post('/api/availability/exceptions', exc, {
      params: { tenantId, resourceId },
    });
    return response.data;
  },

  // GET /api/availability/exceptions?resourceId=
  getExceptions: async (resourceId: string) => {
    const response = await api.get('/api/availability/exceptions', {
      params: { resourceId },
    });
    return response.data;
  },

  // PUT /api/availability/exceptions/{exceptionId}?tenantId=
  updateException: async (exceptionId: string, tenantId: string, exc: ExceptionDto) => {
    const response = await api.put(`/api/availability/exceptions/${exceptionId}`, exc, {
      params: { tenantId },
    });
    return response.data;
  },

  // DELETE /api/availability/exceptions/{exceptionId}?tenantId=
  deleteException: async (exceptionId: string, tenantId: string) => {
    await api.delete(`/api/availability/exceptions/${exceptionId}`, {
      params: { tenantId },
    });
  },
};
