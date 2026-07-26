package com.system.booking.modules.availability.internal.service;

import com.system.booking.modules.availability.api.ScheduleRuleDto;
import com.system.booking.modules.availability.internal.entity.ScheduleRule;
import com.system.booking.modules.availability.internal.repository.ScheduleRuleRepository;
import com.system.booking.modules.inventory.internal.entity.Resource;
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
    public void defineScheduleRule(UUID tenantId, Resource resource, ScheduleRuleDto dto) {
        // Simple overlap validation could go here
        
        ScheduleRule rule = ScheduleRule.builder()
                .tenantId(tenantId)
                .resource(resource)
                .dayOfWeek(dto.dayOfWeek())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .build();
                
        scheduleRuleRepository.save(rule);
    }
    
    @Transactional(readOnly = true)
    public List<ScheduleRule> listRulesForResource(UUID resourceId) {
        return scheduleRuleRepository.findByResourceId(resourceId);
    }
}
