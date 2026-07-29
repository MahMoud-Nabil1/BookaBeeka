package com.system.booking.modules.availability.internal.repository;

import com.system.booking.modules.availability.internal.entity.ScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, UUID> {
    List<ScheduleRule> findByResourceId(UUID resourceId);
    List<ScheduleRule> findByResourceIdAndDayOfWeek(UUID resourceId, Short dayOfWeek);
}
