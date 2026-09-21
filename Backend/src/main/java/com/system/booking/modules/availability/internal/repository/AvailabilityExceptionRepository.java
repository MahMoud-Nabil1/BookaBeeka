package com.system.booking.modules.availability.internal.repository;
import com.system.booking.modules.availability.internal.entity.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {
    List<AvailabilityException> findByResourceIdAndExceptionDate(UUID resourceId, LocalDate exceptionDate);
    List<AvailabilityException> findByResourceId(UUID resourceId);

    List<AvailabilityException> findByTenantIdAndStartDateIsNotNull(UUID tenantId);

    Optional<AvailabilityException> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT ae FROM AvailabilityException ae " +
           "WHERE ae.resource.id = :resourceId " +
           "AND ae.isAvailable = false " +
           "AND ae.startDate IS NOT NULL " +
           "AND ae.startDate < :endDate " +
           "AND ae.endDate > :startDate")
    List<AvailabilityException> findOverlappingBlocks(
            @Param("resourceId") UUID resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
