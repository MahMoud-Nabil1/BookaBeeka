package com.system.booking.modules.tenant.api;

import com.system.booking.modules.security.model.principal.StaffPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import com.system.booking.modules.tenant.api.dto.BranchDto;
import com.system.booking.modules.tenant.api.dto.CreateBranchRequest;
import com.system.booking.modules.tenant.api.dto.UpdateBranchRequest;
import com.system.booking.modules.tenant.internal.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public BranchDto createBranch(@Valid @RequestBody CreateBranchRequest request) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchService.createBranch(principal.tenantId(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public List<BranchDto> listBranches() {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchService.listBranchesByTenant(principal.tenantId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public BranchDto getBranchById(@PathVariable UUID id) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchService.getBranchById(principal.tenantId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public BranchDto updateBranch(@PathVariable UUID id, @RequestBody UpdateBranchRequest request) {
        StaffPrincipal principal = SecurityUtil.getCurrentStaffPrincipal();
        return branchService.updateBranch(principal.tenantId(), id, request);
    }
}
