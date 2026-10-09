package com.system.booking.modules.superAdmin.api;

import com.system.booking.modules.superAdmin.api.dto.*;
import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.superAdmin.internal.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/super")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(superAdminService.getPlatformStats());
    }

    @GetMapping("/tenants")
    public ResponseEntity<Page<TenantSummaryResponse>> listTenants(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listTenants(PageRequest.of(page, size)));
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantDetailResponse> getTenantDetail(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(superAdminService.getTenantDetail(tenantId));
    }

    @PatchMapping("/tenants/{tenantId}/status")
    public ResponseEntity<TenantSummaryResponse> updateTenantStatus(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantStatusRequest request) {
        return ResponseEntity.ok(superAdminService.updateTenantStatus(tenantId, request.status()));
    }

    @PostMapping("/tenants/{tenantId}/suspend")
    public ResponseEntity<TenantSummaryResponse> suspendTenant(
            @PathVariable UUID tenantId,
            @RequestBody(required = false) SuspendTenantRequest request,
            Authentication auth) {
        String actor = resolveActor(auth);
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(superAdminService.suspendTenant(tenantId, reason, actor));
    }

    @PostMapping("/tenants/{tenantId}/unsuspend")
    public ResponseEntity<TenantSummaryResponse> unsuspendTenant(
            @PathVariable UUID tenantId,
            Authentication auth) {
        String actor = resolveActor(auth);
        return ResponseEntity.ok(superAdminService.unsuspendTenant(tenantId, actor));
    }

    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Map<String, String>> deleteTenant(
            @PathVariable UUID tenantId,
            Authentication auth) {
        String actor = resolveActor(auth);
        superAdminService.deleteTenant(tenantId, actor);
        return ResponseEntity.ok(Map.of("message", "Hotel successfully deleted"));
    }

    @GetMapping("/customers")
    public ResponseEntity<Page<CustomerSummaryResponse>> listCustomers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listCustomers(PageRequest.of(page, size)));
    }

    @PatchMapping("/customers/{customerId}/ban")
    public ResponseEntity<CustomerSummaryResponse> banCustomer(
            @PathVariable UUID customerId,
            Authentication auth) {
        String actor = resolveActor(auth);
        return ResponseEntity.ok(superAdminService.banCustomer(customerId, null, actor));
    }

    @PostMapping("/customers/{customerId}/ban")
    public ResponseEntity<CustomerSummaryResponse> banCustomerWithReason(
            @PathVariable UUID customerId,
            @RequestBody(required = false) BanCustomerRequest request,
            Authentication auth) {
        String actor = resolveActor(auth);
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(superAdminService.banCustomer(customerId, reason, actor));
    }

    @PatchMapping("/customers/{customerId}/unban")
    public ResponseEntity<CustomerSummaryResponse> unbanCustomer(
            @PathVariable UUID customerId,
            Authentication auth) {
        String actor = resolveActor(auth);
        return ResponseEntity.ok(superAdminService.unbanCustomer(customerId, actor));
    }

    @PostMapping("/customers/{customerId}/unban")
    public ResponseEntity<CustomerSummaryResponse> unbanCustomerPost(
            @PathVariable UUID customerId,
            Authentication auth) {
        String actor = resolveActor(auth);
        return ResponseEntity.ok(superAdminService.unbanCustomer(customerId, actor));
    }

    @DeleteMapping("/customers/{customerId}")
    public ResponseEntity<Map<String, String>> deleteCustomer(
            @PathVariable UUID customerId,
            Authentication auth) {
        String actor = resolveActor(auth);
        superAdminService.deleteCustomer(customerId, actor);
        return ResponseEntity.ok(Map.of("message", "Customer successfully deleted"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<PlatformTransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listTransactions(PageRequest.of(page, size)));
    }

    @GetMapping("/payments/failed")
    public ResponseEntity<Page<FailedPaymentResponse>> listFailedPayments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listFailedPayments(PageRequest.of(page, size)));
    }

    @GetMapping("/bookings/stuck")
    public ResponseEntity<Page<StuckBookingResponse>> listStuckBookings(
            @RequestParam(defaultValue = "30") int thresholdMinutes,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listStuckBookings(thresholdMinutes, PageRequest.of(page, size)));
    }

    @GetMapping("/wallets/tenants")
    public ResponseEntity<Page<?>> listTenantWallets(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listTenantWallets(PageRequest.of(page, size)));
    }

    @GetMapping("/wallets/customers")
    public ResponseEntity<Page<CustomerWalletSummaryResponse>> listCustomerWallets(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listCustomerWallets(PageRequest.of(page, size)));
    }

    // GET /api/admin/super/tenants/{tenantId}/rooms
    @GetMapping("/tenants/{tenantId}/rooms")
    public ResponseEntity<List<ResourceResponse>> listRoomsByTenant(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(superAdminService.listRoomsByTenant(tenantId));
    }

    private String resolveActor(Authentication auth) {
        if (auth == null) return "SUPER_ADMIN";
        if (auth.getPrincipal() instanceof HotelUserPrincipal p) {
            if (p.email() != null && !p.email().isBlank()) {
                return truncate(p.email(), 100);
            }
            if (p.id() != null) {
                return truncate(p.id().toString(), 100);
            }
        }
        String name = auth.getName();
        if (name == null || name.isBlank()) {
            return "SUPER_ADMIN";
        }
        return truncate(name, 100);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}