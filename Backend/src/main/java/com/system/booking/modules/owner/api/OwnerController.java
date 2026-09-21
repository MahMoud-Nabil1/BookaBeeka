package com.system.booking.modules.owner.api;

import com.system.booking.modules.owner.internal.dto.*;
import com.system.booking.modules.owner.internal.service.OwnerService;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    // ── Owner Self-Registration (Public Endpoint) ───────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AppointAdminResponse> registerOwner(
            @Valid @RequestBody OwnerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.registerOwner(request));
    }

    // ── Admin Management (Protected) ────────────────────────────────────────

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/admins")
    public ResponseEntity<AppointAdminResponse> appointAdmin(
            @AuthenticationPrincipal HotelUserPrincipal principal,
            @Valid @RequestBody AppointAdminRequest request) {
        AppointAdminResponse response = ownerService.appointAdmin(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/admins")
    public ResponseEntity<List<OwnerAdminSummaryDto>> listAdmins(
            @AuthenticationPrincipal HotelUserPrincipal principal) {
        return ResponseEntity.ok(ownerService.listAdmins(principal.tenantId()));
    }

    // ── Dashboard (Protected) ───────────────────────────────────────────────

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/dashboard")
    public ResponseEntity<OwnerDashboardResponse> getDashboard(
            @AuthenticationPrincipal HotelUserPrincipal principal) {
        return ResponseEntity.ok(ownerService.getDashboard(principal.tenantId()));
    }

    // ── Revenue (Protected) ─────────────────────────────────────────────────

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/revenue")
    public ResponseEntity<OwnerRevenueSummaryDto> getRevenueSummary(
            @AuthenticationPrincipal HotelUserPrincipal principal) {
        return ResponseEntity.ok(ownerService.getRevenueSummary(principal.tenantId()));
    }
}