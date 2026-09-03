package com.system.booking.modules.availability.internal.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class RoomAvailabilityRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Searches for available hotel rooms using date-range overlap logic.
     * Supports cross-hotel discovery (hotelId=null) and hotel-specific search.
     * Filters: roomType, bedType, minCapacity, price range, amenity UUIDs.
     */
    public Page<Object[]> searchAvailableRooms(
            UUID hotelId,
            LocalDate checkIn,
            LocalDate checkOut,
            String roomType,
            String bedType,
            Integer minCapacity,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<UUID> amenityIds,
            Pageable pageable) {

        int amenityCount = (amenityIds != null) ? amenityIds.size() : 0;

        String baseSql = buildBaseQuery(amenityCount > 0);
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS count_query";
        String pagedSql = baseSql + " ORDER BY r.price_per_night ASC NULLS LAST";

        // Count query
        Query countQuery = entityManager.createNativeQuery(countSql);
        setParameters(countQuery, hotelId, checkIn, checkOut, roomType, bedType,
                minCapacity, minPrice, maxPrice, amenityIds, amenityCount);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Data query with pagination
        Query dataQuery = entityManager.createNativeQuery(pagedSql);
        setParameters(dataQuery, hotelId, checkIn, checkOut, roomType, bedType,
                minCapacity, minPrice, maxPrice, amenityIds, amenityCount);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> results = dataQuery.getResultList();

        return new PageImpl<>(results, pageable, total);
    }

    private String buildBaseQuery(boolean hasAmenities) {
        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT
                r.id AS room_id,
                r.name AS room_name,
                r.resource_type,
                r.capacity,
                r.specs,
                r.price_per_night,
                r.currency,
                t.id AS hotel_id,
                t.name AS hotel_name,
                t.subdomain
            FROM resource r
            JOIN tenant t ON r.tenant_id = t.id
            WHERE r.is_bookable = true
              AND r.is_active = true
              AND t.status = 'ACTIVE'
              AND (:hotelId IS NULL OR r.tenant_id = CAST(:hotelId AS uuid))
              AND (:roomType IS NULL OR r.resource_type = :roomType)
              AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
              AND (:bedType IS NULL OR r.specs->>'bedType' = :bedType)
              AND (:minPrice IS NULL OR r.price_per_night >= CAST(:minPrice AS numeric))
              AND (:maxPrice IS NULL OR r.price_per_night <= CAST(:maxPrice AS numeric))
            """);

        // Amenity filter: room must have ALL requested amenities
        if (hasAmenities) {
            sql.append("""
              AND :amenityCount = (
                  SELECT COUNT(DISTINCT ra.amenity_id)
                  FROM resource_amenity ra
                  WHERE ra.resource_id = r.id
                    AND ra.amenity_id IN (:amenityIds)
              )
            """);
        }

        // No overlapping active hotel bookings
        sql.append("""
              AND NOT EXISTS (
                  SELECT 1 FROM booking b
                  WHERE b.resource_id = r.id
                    AND b.status IN ('PENDING_PAYMENT', 'CONFIRMED')
                    AND b.check_in IS NOT NULL
                    AND b.check_in < CAST(:checkOut AS date)
                    AND b.check_out > CAST(:checkIn AS date)
              )
            """);

        // No overlapping active room blocks
        sql.append("""
              AND NOT EXISTS (
                  SELECT 1 FROM availability_exceptions ae
                  WHERE ae.resource_id = r.id
                    AND ae.is_available = false
                    AND ae.start_date IS NOT NULL
                    AND ae.start_date < CAST(:checkOut AS date)
                    AND ae.end_date > CAST(:checkIn AS date)
              )
            """);

        return sql.toString();
    }

    private void setParameters(
            Query query,
            UUID hotelId,
            LocalDate checkIn,
            LocalDate checkOut,
            String roomType,
            String bedType,
            Integer minCapacity,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<UUID> amenityIds,
            int amenityCount) {

        query.setParameter("hotelId", hotelId != null ? hotelId.toString() : null);
        query.setParameter("checkIn", checkIn);
        query.setParameter("checkOut", checkOut);
        query.setParameter("roomType", roomType);
        query.setParameter("minCapacity", minCapacity);
        query.setParameter("bedType", bedType);
        query.setParameter("minPrice", minPrice != null ? minPrice.toString() : null);
        query.setParameter("maxPrice", maxPrice != null ? maxPrice.toString() : null);

        if (amenityCount > 0) {
            // Convert UUIDs to strings for PostgreSQL IN clause
            List<String> amenityIdStrings = amenityIds.stream()
                    .map(UUID::toString)
                    .toList();
            query.setParameter("amenityIds", amenityIdStrings);
            query.setParameter("amenityCount", amenityCount);
        }
    }

    /**
     * Quick check: is a specific room available for the given date range?
     */
    public boolean isRoomAvailable(UUID resourceId, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM booking b
                WHERE b.resource_id = CAST(:resourceId AS uuid)
                  AND b.status IN ('PENDING_PAYMENT', 'CONFIRMED')
                  AND b.check_in IS NOT NULL
                  AND b.check_in < CAST(:checkOut AS date)
                  AND b.check_out > CAST(:checkIn AS date)
            ) OR EXISTS (
                SELECT 1 FROM availability_exceptions ae
                WHERE ae.resource_id = CAST(:resourceId AS uuid)
                  AND ae.is_available = false
                  AND ae.start_date IS NOT NULL
                  AND ae.start_date < CAST(:checkOut AS date)
                  AND ae.end_date > CAST(:checkIn AS date)
            ) THEN false ELSE true END
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("resourceId", resourceId.toString());
        query.setParameter("checkIn", checkIn);
        query.setParameter("checkOut", checkOut);

        return (Boolean) query.getSingleResult();
    }
}
