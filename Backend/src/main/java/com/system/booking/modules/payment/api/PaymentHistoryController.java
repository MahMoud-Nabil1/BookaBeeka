package com.system.booking.modules.payment.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for payment history and balance inquiries.
 *
 * <p>Talks only to {@link PaymentModuleApi}, consistent with {@link PaymentController}
 * — no direct dependency on the internal {@code payment.internal} package.</p>
 *
 * <p>Base path: {@code /api/payments}</p>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentModuleApi paymentModuleApi;

    // -------------------------------------------------------------------------
    // GET /api/payments/history/customer/{customerId}
    // -------------------------------------------------------------------------

    /**
     * Returns the complete, ordered wallet transaction history for a customer (Paginated).
     */
    @GetMapping("/history/customer/{customerId}")
    public ResponseEntity<Page<CustomerTransactionDetail>> getCustomerHistory(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CustomerTransactionDetail> history = paymentModuleApi.getCustomerHistory(customerId, page, size);
        return ResponseEntity.ok(history);
    }

    // -------------------------------------------------------------------------
    // GET /api/payments/customer/{customerId}/balance
    // -------------------------------------------------------------------------

    /**
     * Returns the current balance snapshot for a customer's global wallet.
     */
    @GetMapping("/customer/{customerId}/balance")
    public ResponseEntity<CustomerBalanceResponse> getCustomerBalance(@PathVariable UUID customerId) {
        CustomerBalanceResponse balance = paymentModuleApi.getCustomerBalance(customerId);
        return ResponseEntity.ok(balance);
    }

    // -------------------------------------------------------------------------
    // GET /api/payments/history/tenant/{tenantId}
    // -------------------------------------------------------------------------

    /**
     * Returns all payment records for every booking that belongs to a given tenant (Paginated).
     */
    @GetMapping("/history/tenant/{tenantId}")
    public ResponseEntity<Page<TenantPaymentDetail>> getTenantPaymentHistory(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<TenantPaymentDetail> history = paymentModuleApi.getTenantPaymentHistory(tenantId, page, size);
        return ResponseEntity.ok(history);
    }

    // -------------------------------------------------------------------------
    // GET /api/payments/tenant/{tenantId}/balance
    // -------------------------------------------------------------------------

    /**
     * Returns the tenant's current accumulated revenue balance.
     */
    @GetMapping("/tenant/{tenantId}/balance")
    public ResponseEntity<TenantBalanceResponse> getTenantBalance(
            @PathVariable UUID tenantId) {

        TenantBalanceResponse balance = paymentModuleApi.getTenantBalance(tenantId);
        return ResponseEntity.ok(balance);
    }
}