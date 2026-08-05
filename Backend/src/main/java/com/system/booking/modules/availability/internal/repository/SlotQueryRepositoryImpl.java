package com.system.booking.modules.availability.internal.repository;

import com.system.booking.modules.availability.api.SlotDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class SlotQueryRepositoryImpl implements SlotQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SlotDto> generateSlots(UUID tenantId, UUID resourceId, LocalDate date, int durationMinutes, int bufferMinutes) {
        String sql = """
            WITH candidate_slots AS (
                SELECT generate_series(
                    (CAST(:date AS date) + start_time) AT TIME ZONE 'UTC',
                    (CAST(:date AS date) + end_time) AT TIME ZONE 'UTC' - CAST(:duration AS interval),
                    CAST(:duration AS interval)
                ) AS slot_start
                FROM schedule_rules
                WHERE resource_id = CAST(:resourceId AS uuid)
                  AND tenant_id = CAST(:tenantId AS uuid)
                  AND day_of_week = EXTRACT(ISODOW FROM CAST(:date AS date))
            ),
            blocked AS (
                SELECT tstzrange(start_time, end_time + CAST(:buffer AS interval)) AS range
                FROM booking
                WHERE resource_id = CAST(:resourceId AS uuid)
                  AND tenant_id = CAST(:tenantId AS uuid)
                  AND status NOT IN ('CANCELLED', 'EXPIRED')
                UNION ALL
                SELECT tstzrange(slot_start, slot_end)
                FROM slot_locks
                WHERE resource_id = CAST(:resourceId AS uuid)
                  AND tenant_id = CAST(:tenantId AS uuid)
                  AND status = 'ACTIVE' AND expires_at > now()
            )
            SELECT slot_start, slot_start + CAST(:duration AS interval) AS slot_end
            FROM candidate_slots
            WHERE NOT EXISTS (
                SELECT 1 FROM blocked WHERE range && tstzrange(slot_start, slot_start + CAST(:duration AS interval))
            )
            AND NOT EXISTS (
                SELECT 1 FROM availability_exceptions
                WHERE resource_id = CAST(:resourceId AS uuid)
                  AND tenant_id = CAST(:tenantId AS uuid)
                  AND exception_date = CAST(:date AS date) AND is_available = false
            )
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("date", date)
            .setParameter("resourceId", resourceId.toString())
            .setParameter("tenantId", tenantId.toString())
            .setParameter("duration", durationMinutes + " minutes")
            .setParameter("buffer", bufferMinutes + " minutes");

        List<Object[]> results = query.getResultList();
        List<SlotDto> slots = new ArrayList<>();
        
        for (Object[] row : results) {
            OffsetDateTime start;
            OffsetDateTime end;
            
            if (row[0] instanceof Timestamp ts) {
                start = ts.toInstant().atOffset(ZoneOffset.UTC);
                end = ((Timestamp) row[1]).toInstant().atOffset(ZoneOffset.UTC);
            } else if (row[0] instanceof OffsetDateTime) {
                start = (OffsetDateTime) row[0];
                end = (OffsetDateTime) row[1];
            } else if (row[0] instanceof java.time.Instant inst) {
                start = inst.atOffset(ZoneOffset.UTC);
                end = ((java.time.Instant) row[1]).atOffset(ZoneOffset.UTC);
            } else if (row[0] instanceof java.time.ZonedDateTime zdt) {
                start = zdt.toOffsetDateTime();
                end = ((java.time.ZonedDateTime) row[1]).toOffsetDateTime();
            } else if (row[0] instanceof java.time.LocalDateTime ldt) {
                start = ldt.atOffset(ZoneOffset.UTC);
                end = ((java.time.LocalDateTime) row[1]).atOffset(ZoneOffset.UTC);
            } else {
                throw new IllegalStateException("Unexpected timestamp type from database: " + row[0].getClass().getName());
            }
            
            slots.add(new SlotDto(start, end));
        }
        
        return slots;
    }
}
