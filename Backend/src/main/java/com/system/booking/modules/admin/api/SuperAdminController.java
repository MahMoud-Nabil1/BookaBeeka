package com.system.booking.modules.admin.api;

import com.system.booking.modules.admin.api.dto.*;
import com.system.booking.modules.admin.internal.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * SuperAdmin REST API — all endpoints are platform-wide and cross-tenant.
 *
 * <p>Base path: {@code /api/admin/super}</p>
 *
 * <p><b>TODO for security team:</b> Add {@code @EnableMethodSecurity} to
 * {@code SecurityConfig} and wire the JWT filter to populate
 * {@code SecurityContextHolder} with {@code ROLE_SUPER_ADMIN} for tokens
 * carrying {@code user_type = "SUPER_ADMIN"}. Then uncomment the
 * {@code @PreAuthorize} annotations below to enforce access control.</p>
 */
@RestController
@RequestMapping("/api/admin/super")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    // -------------------------------------------------------------------------
    // POST /api/admin/super/login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a SuperAdmin and returns a JWT token.
     *
     * <p>Request body: {@code { "email": "admin@platform.com", "password": "..." }}
     *
     * @return 200 OK with {@code { "token": "...", "userType": "SUPER_ADMIN" }}
     */
    @PostMapping("/login")
    public ResponseEntity<SuperAdminLoginResponse> login(
            @Valid @RequestBody SuperAdminLoginRequest request) {
        return ResponseEntity.ok(superAdminService.login(request));
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/super/stats
    // -------------------------------------------------------------------------

    /**
     * Platform-wide KPI snapshot: tenant counts, customer counts, booking stats,
     * revenue totals, failed payments count, stuck bookings count.
     *
     * @return 200 OK with {@link PlatformStatsResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")  // TODO: uncomment after JWT filter is wired
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(superAdminService.getPlatformStats());
    }

    // -------------------------------------------------------------------------
    // Tenant Management
    // -------------------------------------------------------------------------

    /**
     * Paginated list of all tenants — for the SuperAdmin tenant management table.
     *
     * @param page page index (0-based), default 0
     * @param size page size, default 20
     * @return 200 OK with page of {@link TenantSummaryResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/tenants")
    public ResponseEntity<Page<TenantSummaryResponse>> listTenants(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listTenants(PageRequest.of(page, size)));
    }

    /**
     * Full tenant detail including financial and booking stats.
     *
     * @param tenantId UUID of the tenant
     * @return 200 OK with {@link TenantDetailResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantDetailResponse> getTenantDetail(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(superAdminService.getTenantDetail(tenantId));
    }

    /**
     * Updates a tenant's status.
     *
     * <p>Request body: {@code { "status": "ACTIVE" | "SUSPENDED" | "BANNED" }}
     *
     * @param tenantId  UUID of the tenant to update
     * @param request   body containing the new status string
     * @return 200 OK with updated {@link TenantSummaryResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/tenants/{tenantId}/status")
    public ResponseEntity<TenantSummaryResponse> updateTenantStatus(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantStatusRequest request) {
        return ResponseEntity.ok(superAdminService.updateTenantStatus(tenantId, request.status()));
    }

    // -------------------------------------------------------------------------
    // Customer Management
    // -------------------------------------------------------------------------

    /**
     * Paginated list of all customers including ban status and wallet balance.
     *
     * @param page page index (0-based), default 0
     * @param size page size, default 20
     * @return 200 OK with page of {@link CustomerSummaryResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/customers")
    public ResponseEntity<Page<CustomerSummaryResponse>> listCustomers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listCustomers(PageRequest.of(page, size)));
    }

    /**
     * Bans a customer — sets isActive=false, blocking future logins.
     *
     * @param customerId UUID of the customer to ban
     * @return 200 OK with updated {@link CustomerSummaryResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/customers/{customerId}/ban")
    public ResponseEntity<CustomerSummaryResponse> banCustomer(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(superAdminService.banCustomer(customerId));
    }

    /**
     * Unbans a customer — restores isActive=true.
     *
     * @param customerId UUID of the customer to unban
     * @return 200 OK with updated {@link CustomerSummaryResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/customers/{customerId}/unban")
    public ResponseEntity<CustomerSummaryResponse> unbanCustomer(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(superAdminService.unbanCustomer(customerId));
    }

    // -------------------------------------------------------------------------
    // Transaction Audit (Money-flow)
    // -------------------------------------------------------------------------

    /**
     * Platform-wide paginated transaction feed showing full money flow:
     * FROM (customer name + email) → amount → TO (tenantId, nullable for deposits).
     *
     * @param page page index (0-based), default 0
     * @param size page size, default 20
     * @return 200 OK with page of {@link PlatformTransactionResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/transactions")
    public ResponseEntity<Page<PlatformTransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listTransactions(PageRequest.of(page, size)));
    }

    // -------------------------------------------------------------------------
    // Failed Payments
    // -------------------------------------------------------------------------

    /**
     * Paginated feed of all FAILED payments across all tenants.
     * Use this to spot abuse patterns or persistent payment processing issues.
     *
     * @param page page index (0-based), default 0
     * @param size page size, default 20
     * @return 200 OK with page of {@link FailedPaymentResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/payments/failed")
    public ResponseEntity<Page<FailedPaymentResponse>> listFailedPayments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listFailedPayments(PageRequest.of(page, size)));
    }

    // -------------------------------------------------------------------------
    // Stuck Bookings
    // -------------------------------------------------------------------------

    /**
     * Bookings stuck in PENDING_PAYMENT longer than {@code thresholdMinutes}.
     * Default threshold is 30 minutes. Signals a payment processing issue.
     *
     * @param thresholdMinutes how old a PENDING_PAYMENT booking must be to count as stuck (default 30)
     * @param page             page index (0-based), default 0
     * @param size             page size, default 20
     * @return 200 OK with page of {@link StuckBookingResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/bookings/stuck")
    public ResponseEntity<Page<StuckBookingResponse>> listStuckBookings(
            @RequestParam(defaultValue = "30") int thresholdMinutes,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listStuckBookings(thresholdMinutes, PageRequest.of(page, size)));
    }

    // -------------------------------------------------------------------------
    // Wallet Overviews
    // -------------------------------------------------------------------------

    /**
     * All tenant revenue wallets sorted by balance descending.
     * Shows which tenants have accumulated the most revenue.
     *
     * @param page page index (0-based), default 0
     * @param size page size, default 20
     * @return 200 OK with page of tenant wallet data
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/wallets/tenants")
    public ResponseEntity<Page<?>> listTenantWallets(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listTenantWallets(PageRequest.of(page, size)));
    }

    /**
     * All customer wallets sorted by balance descending.
     * Shows which customers currently hold the most funds in their global wallet.
     *
     * @param page page index (0-based), default 0
     * @param size page size, default 20
     * @return 200 OK with page of {@link CustomerWalletSummaryResponse}
     */
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/wallets/customers")
    public ResponseEntity<Page<CustomerWalletSummaryResponse>> listCustomerWallets(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(superAdminService.listCustomerWallets(PageRequest.of(page, size)));
    }
}
