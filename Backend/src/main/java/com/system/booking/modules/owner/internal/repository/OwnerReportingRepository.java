package com.system.booking.modules.owner.internal.repository;

import com.system.booking.modules.owner.internal.dto.OwnerAdminSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OwnerReportingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal getOverallTenantRevenue(UUID tenantId) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0)
            FROM booking
            WHERE tenant_id = ? AND status IN ('CONFIRMED', 'COMPLETED')
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, tenantId);
    }

    public long getTotalBookingCount(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM booking WHERE tenant_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    public long getCompletedBookingCount(UUID tenantId) {
        String sql = "SELECT COUNT(*) FROM booking WHERE tenant_id = ? AND status IN ('CONFIRMED', 'COMPLETED')";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    public List<OwnerAdminSummaryDto> listTenantAdmins(UUID tenantId) {
        String sql = """
            SELECT
                a.id AS admin_id,
                a.first_name,
                a.last_name,
                a.email,
                a.phone,
                'ADMIN' AS role,
                a.tenant_id,
                a.is_active,
                a.created_at,
                a.updated_at
            FROM hotel_admin a
            WHERE a.tenant_id = ?
            ORDER BY a.created_at DESC
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OwnerAdminSummaryDto(
                        rs.getObject("admin_id", UUID.class),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getBoolean("is_active"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getObject("updated_at", LocalDateTime.class)
                ),
                tenantId
        );
    }
}