package com.system.booking.modules.tenant.api;

import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.tenant.internal.dto.TenantDto;
import com.system.booking.modules.tenant.internal.dto.UpdateTenantRequestDto;
import com.system.booking.modules.tenant.internal.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tenant profile operations.
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // ── Profile Endpoints (Owner only) ──────────────────────────────────────

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/me")
    public ResponseEntity<TenantDto> getMyTenant(
            @AuthenticationPrincipal HotelUserPrincipal principal) {
        return ResponseEntity.ok(tenantService.getTenantById(principal.tenantId()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/me")
    public ResponseEntity<TenantDto> updateMyTenant(
            @AuthenticationPrincipal HotelUserPrincipal principal,
            @RequestBody UpdateTenantRequestDto request) {
        return ResponseEntity.ok(tenantService.updateTenant(principal.tenantId(), request));
    }

    // ── Public Endpoint ─────────────────────────────────────────────────────

    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<TenantDto> getTenantBySubdomain(
            @PathVariable String subdomain) {
        return ResponseEntity.ok(tenantService.getTenantBySubdomain(subdomain));
    }
}