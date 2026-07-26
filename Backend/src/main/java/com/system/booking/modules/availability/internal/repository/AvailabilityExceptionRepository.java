package com.system.booking.modules.availability.internal.repository;

import com.system.booking.modules.availability.internal.entity.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {
    List<AvailabilityException> findByResourceIdAndExceptionDate(UUID resourceId, LocalDate exceptionDate);
}
