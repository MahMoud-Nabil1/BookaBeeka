package com.system.booking.modules.availability.internal.service;
import com.system.booking.modules.availability.api.ScheduleRuleDto;
import com.system.booking.modules.availability.internal.entity.ScheduleRule;
import com.system.booking.modules.availability.internal.repository.ScheduleRuleRepository;
import com.system.booking.modules.inventory.internal.entity.Resource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleRuleService {
    private final ScheduleRuleRepository scheduleRuleRepository;

    @Transactional
    public ScheduleRule defineScheduleRule(UUID tenantId, Resource resource, ScheduleRuleDto dto) {
        ScheduleRule rule = ScheduleRule.builder()
                .tenantId(tenantId).resource(resource)
                .dayOfWeek(dto.dayOfWeek()).startTime(dto.startTime()).endTime(dto.endTime())
                .build();
        return scheduleRuleRepository.save(rule);
    }
    @Transactional(readOnly = true)
    public List<ScheduleRule> listRulesForResource(UUID resourceId) {
        return scheduleRuleRepository.findByResourceId(resourceId);
    }

    @Transactional
    public void updateScheduleRule(UUID tenantId, UUID ruleId, ScheduleRuleDto dto) {
        ScheduleRule rule = scheduleRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule rule not found"));
        if (!rule.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Schedule rule not found for tenant");
        }
        rule.setDayOfWeek(dto.dayOfWeek());
        rule.setStartTime(dto.startTime());
        rule.setEndTime(dto.endTime());
        scheduleRuleRepository.save(rule);
    }

    @Transactional
    public void deleteScheduleRule(UUID tenantId, UUID ruleId) {
        ScheduleRule rule = scheduleRuleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule rule not found"));
        if (!rule.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Schedule rule not found for tenant");
        }
        scheduleRuleRepository.delete(rule);
    }
}
