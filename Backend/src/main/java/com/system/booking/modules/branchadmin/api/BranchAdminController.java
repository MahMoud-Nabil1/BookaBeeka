package com.system.booking.modules.branchadmin.api;

import com.system.booking.modules.booking.api.CancellationResultDto;
import com.system.booking.modules.branchadmin.api.dto.BranchBookingResponse;
import com.system.booking.modules.branchadmin.api.dto.BranchDashboardResponse;
import com.system.booking.modules.branchadmin.internal.service.BranchAdminService;
import com.system.booking.modules.security.model.principal.StaffPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branch-admin")
@RequiredArgsConstructor
public class BranchAdminController {

    private final BranchAdminService branchAdminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public BranchDashboardResponse getDashboard() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchAdminService.getDashboard(principal.tenantId(), principal.branchId(), "Branch");
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public List<BranchBookingResponse> listBookings() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchAdminService.listBookings(principal.tenantId(), principal.branchId());
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void confirmBooking(@PathVariable UUID bookingId) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        branchAdminService.confirmBooking(principal.tenantId(), bookingId);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public CancellationResultDto cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String reason) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchAdminService.cancelBooking(principal.tenantId(), bookingId, reason, principal.id());
    }
}
