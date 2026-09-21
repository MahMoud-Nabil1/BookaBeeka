package com.system.booking.modules.superAdmin.api;

import com.system.booking.modules.superAdmin.api.dto.*;
import com.system.booking.modules.superAdmin.internal.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/customers")
    public ResponseEntity<Page<CustomerSummaryResponse>> listCustomers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listCustomers(PageRequest.of(page, size)));
    }

    @PatchMapping("/customers/{customerId}/ban")
    public ResponseEntity<CustomerSummaryResponse> banCustomer(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(superAdminService.banCustomer(customerId));
    }

    @PatchMapping("/customers/{customerId}/unban")
    public ResponseEntity<CustomerSummaryResponse> unbanCustomer(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(superAdminService.unbanCustomer(customerId));
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
}