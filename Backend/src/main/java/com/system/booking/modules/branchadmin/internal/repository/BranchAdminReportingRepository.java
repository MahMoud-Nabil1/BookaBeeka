package com.system.booking.modules.branchadmin.internal.repository;

import com.system.booking.modules.branchadmin.api.dto.BranchBookingResponse;
import com.system.booking.modules.branchadmin.api.dto.BranchDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BranchAdminReportingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BranchDashboardResponse getDashboardStats(UUID tenantId, UUID branchId, String branchName) {
        String sql = """
            SELECT count(*) as totalBookings, coalesce(sum(total_amount), 0) as totalRevenue
            FROM booking
            WHERE tenant_id = ? AND branch_id = ? AND DATE(created_at) = ?
            """;
        
        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new BranchDashboardResponse(
                        branchName,
                        rs.getLong("totalBookings"),
                        rs.getBigDecimal("totalRevenue")
                ),
                tenantId, branchId, LocalDate.now()
        );
    }

    public List<BranchBookingResponse> listBranchBookings(UUID tenantId, UUID branchId) {
        String sql = """
            SELECT id, customer_id, resource_id, start_time, end_time, status, total_amount, currency, created_at
            FROM booking
            WHERE tenant_id = ? AND branch_id = ?
            ORDER BY created_at DESC
            """;
            
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new BranchBookingResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        rs.getObject("resource_id", UUID.class),
                        rs.getObject("start_time", OffsetDateTime.class),
                        rs.getObject("end_time", OffsetDateTime.class),
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("currency"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                tenantId, branchId
        );
    }
}
