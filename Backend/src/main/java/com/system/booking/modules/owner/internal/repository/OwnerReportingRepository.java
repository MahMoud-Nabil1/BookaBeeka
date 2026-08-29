package com.system.booking.modules.owner.internal.repository;

import com.system.booking.modules.owner.api.dto.BranchRevenueBreakdownDto;
import com.system.booking.modules.owner.api.dto.OwnerAdminSummaryDto;
import com.system.booking.modules.owner.api.dto.OwnerBranchSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for Owner-specific aggregation queries.
 *
 * <p>Uses {@link JdbcTemplate} for high-performance SQL aggregations across
 * {@code booking}, {@code branch}, {@code staff}, and {@code tenant_wallet}.</p>
 */
@Repository
@RequiredArgsConstructor
public class OwnerReportingRepository {

    private final JdbcTemplate jdbcTemplate;

    // ── Revenue Queries ─────────────────────────────────────────────────────

    /**
     * Computes overall tenant revenue from confirmed/completed bookings.
     */
    public BigDecimal getOverallTenantRevenue(UUID tenantId) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0)
            FROM booking
            WHERE tenant_id = ? AND status IN ('CONFIRMED', 'COMPLETED')
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, tenantId);
    }

    /**
     * Counts total bookings across all statuses for a tenant.
     */
    public long getTotalBookingCount(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM booking WHERE tenant_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    /**
     * Counts completed bookings for a tenant.
     */
    public long getCompletedBookingCount(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM booking WHERE tenant_id = ? AND status IN ('CONFIRMED', 'COMPLETED')";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    // ── Branch Queries ──────────────────────────────────────────────────────

    /**
     * Returns branch summaries with booking and staff aggregations.
     *
     * <p>Joins {@code branch} with aggregated booking and staff counts via
     * subqueries to avoid N+1 performance issues.</p>
     */
    public List<OwnerBranchSummaryDto> getBranchSummaries(UUID tenantId) {
        BigDecimal overallRevenue = getOverallTenantRevenue(tenantId);

        String sql = """
            SELECT
                b.id            AS branch_id,
                b.name          AS branch_name,
                b.address       AS branch_address,
                b.status        AS branch_status,
                COALESCE(staff_agg.staff_count, 0)      AS staff_count,
                COALESCE(booking_agg.total_bookings, 0)  AS total_bookings,
                COALESCE(booking_agg.completed, 0)       AS completed_bookings,
                COALESCE(booking_agg.revenue, 0)         AS branch_revenue
            FROM branch b
            LEFT JOIN (
                SELECT branch_id,
                       COUNT(*)                                                    AS total_bookings,
                       COUNT(*) FILTER (WHERE status IN ('CONFIRMED','COMPLETED')) AS completed,
                       COALESCE(SUM(total_amount) FILTER (WHERE status IN ('CONFIRMED','COMPLETED')), 0) AS revenue
                FROM booking
                WHERE tenant_id = ?
                GROUP BY branch_id
            ) booking_agg ON booking_agg.branch_id = b.id
            LEFT JOIN (
                SELECT branch_id, COUNT(*) AS staff_count
                FROM staff
                WHERE tenant_id = ? AND is_active = true
                GROUP BY branch_id
            ) staff_agg ON staff_agg.branch_id = b.id
            WHERE b.tenant_id = ?
            ORDER BY b.name
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    BigDecimal branchRevenue = rs.getBigDecimal("branch_revenue");
                    double pct = (overallRevenue.compareTo(BigDecimal.ZERO) > 0)
                            ? branchRevenue.multiply(BigDecimal.valueOf(100))
                                .divide(overallRevenue, 2, RoundingMode.HALF_UP)
                                .doubleValue()
                            : 0.0;

                    return new OwnerBranchSummaryDto(
                            rs.getObject("branch_id", UUID.class),
                            rs.getString("branch_name"),
                            rs.getString("branch_address"),
                            rs.getString("branch_status"),
                            null, // settings not loaded in aggregate query
                            rs.getLong("staff_count"),
                            rs.getLong("total_bookings"),
                            rs.getLong("completed_bookings"),
                            branchRevenue,
                            pct
                    );
                },
                tenantId, tenantId, tenantId
        );
    }

    /**
     * Returns branch revenue breakdowns for the revenue detail endpoint.
     */
    public List<BranchRevenueBreakdownDto> getBranchRevenueBreakdown(UUID tenantId) {
        BigDecimal overallRevenue = getOverallTenantRevenue(tenantId);

        String sql = """
            SELECT
                b.id     AS branch_id,
                b.name   AS branch_name,
                b.status AS branch_status,
                COALESCE(agg.revenue, 0)       AS revenue,
                COALESCE(agg.completed, 0)     AS completed_count,
                COALESCE(agg.pending, 0)       AS pending_count,
                CASE WHEN COALESCE(agg.completed, 0) > 0
                     THEN COALESCE(agg.revenue, 0) / agg.completed
                     ELSE 0
                END AS avg_booking_value
            FROM branch b
            LEFT JOIN (
                SELECT branch_id,
                       COALESCE(SUM(total_amount) FILTER (WHERE status IN ('CONFIRMED','COMPLETED')), 0) AS revenue,
                       COUNT(*) FILTER (WHERE status IN ('CONFIRMED','COMPLETED'))  AS completed,
                       COUNT(*) FILTER (WHERE status = 'PENDING_PAYMENT')           AS pending
                FROM booking
                WHERE tenant_id = ?
                GROUP BY branch_id
            ) agg ON agg.branch_id = b.id
            WHERE b.tenant_id = ?
            ORDER BY revenue DESC
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    BigDecimal revenue = rs.getBigDecimal("revenue");
                    double pct = (overallRevenue.compareTo(BigDecimal.ZERO) > 0)
                            ? revenue.multiply(BigDecimal.valueOf(100))
                                .divide(overallRevenue, 2, RoundingMode.HALF_UP)
                                .doubleValue()
                            : 0.0;

                    return new BranchRevenueBreakdownDto(
                            rs.getObject("branch_id", UUID.class),
                            rs.getString("branch_name"),
                            rs.getString("branch_status"),
                            revenue,
                            rs.getLong("completed_count"),
                            rs.getLong("pending_count"),
                            rs.getBigDecimal("avg_booking_value"),
                            pct
                    );
                },
                tenantId, tenantId
        );
    }

    // ── Admin Queries ───────────────────────────────────────────────────────

    /**
     * Lists all Admin staff members for a tenant with full branch details.
     */
    public List<OwnerAdminSummaryDto> listTenantAdmins(UUID tenantId) {
        String sql = """
            SELECT
                s.id          AS staff_id,
                s.first_name,
                s.last_name,
                s.email,
                s.phone,
                s.role,
                s.tenant_id,
                s.is_active,
                s.created_at,
                s.updated_at,
                b.id          AS branch_id,
                b.name        AS branch_name,
                b.address     AS branch_address
            FROM staff s
            JOIN branch b ON b.id = s.branch_id
            WHERE s.tenant_id = ? AND s.role = 'ADMIN'
            ORDER BY s.created_at DESC
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OwnerAdminSummaryDto(
                        rs.getObject("staff_id", UUID.class),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getObject("branch_id", UUID.class),
                        rs.getString("branch_name"),
                        rs.getString("branch_address"),
                        rs.getBoolean("is_active"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getObject("updated_at", LocalDateTime.class)
                ),
                tenantId
        );
    }

    /**
     * Counts active Admin staff members for a tenant.
     */
    public long countAdmins(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM staff WHERE tenant_id = ? AND role = 'ADMIN'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    /**
     * Counts branches for a tenant.
     */
    public long countBranches(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM branch WHERE tenant_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    /**
     * Counts active branches for a tenant.
     */
    public long countActiveBranches(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM branch WHERE tenant_id = ? AND status = 'ACTIVE'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }
}
