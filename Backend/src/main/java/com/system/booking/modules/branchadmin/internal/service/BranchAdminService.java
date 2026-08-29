package com.system.booking.modules.branchadmin.internal.service;

import com.system.booking.modules.booking.api.BookingModuleApi;
import com.system.booking.modules.booking.api.CancellationResultDto;
import com.system.booking.modules.branchadmin.api.dto.BranchBookingResponse;
import com.system.booking.modules.branchadmin.api.dto.BranchDashboardResponse;
import com.system.booking.modules.branchadmin.internal.repository.BranchAdminReportingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchAdminService {

    private final BranchAdminReportingRepository reportingRepository;
    private final BookingModuleApi bookingModuleApi;

    @Transactional(readOnly = true)
    public BranchDashboardResponse getDashboard(UUID tenantId, UUID branchId, String branchName) {
        return reportingRepository.getDashboardStats(tenantId, branchId, branchName);
    }

    @Transactional(readOnly = true)
    public List<BranchBookingResponse> listBookings(UUID tenantId, UUID branchId) {
        return reportingRepository.listBranchBookings(tenantId, branchId);
    }

    @Transactional
    public void confirmBooking(UUID tenantId, UUID bookingId) {
        bookingModuleApi.confirmBooking(tenantId, bookingId);
    }

    @Transactional
    public CancellationResultDto cancelBooking(UUID tenantId, UUID bookingId, String reason, UUID actorId) {
        return bookingModuleApi.cancelBooking(tenantId, bookingId, reason, actorId);
    }
}
