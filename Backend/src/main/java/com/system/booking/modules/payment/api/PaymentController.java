package com.system.booking.modules.payment.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST entry point for wallet-based payment operations.
 *
 * <p>All endpoints delegate exclusively through {@link PaymentModuleApi}, never
 * touching internal services or repositories directly. This keeps the controller
 * thin and the module boundary enforced at compile time.</p>
 *
 * <p>Base path: {@code /api/payments}</p>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    // Controllers talk only to the module API — never to internal services directly
    private final PaymentModuleApi paymentModuleApi;

    // -------------------------------------------------------------------------
    // POST /api/payments/wallet/top-up
    // -------------------------------------------------------------------------

    /**
     * Credits a customer's wallet with the specified amount.
     *
     * <p>Expected request body:
     * <pre>{@code
     * {
     *   "customerId": "uuid",
     *   "tenantId":   "uuid",
     *   "amount":     100.00
     * }
     * }</pre>
     *
     * @param request top-up details
     * @return 201 Created with the resulting ledger entry
     */
    @PostMapping("/wallet/top-up")
    public ResponseEntity<WalletTransactionResponse> topUpWallet(@Valid @RequestBody TopUpRequest request) {
        WalletTransactionResponse response = paymentModuleApi.topUpWallet(
                request.customerId(),
                request.amount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/payments/wallet/checkout
    // -------------------------------------------------------------------------

    /**
     * Initiates a wallet payment for a specific booking.
     *
     * <p>Expected request body:
     * <pre>{@code
     * {
     *   "bookingId":       "uuid",
     *   "customerId":      "uuid",
     *   "tenantId":        "uuid",
     *   "paymentAmount":   250.00,
     *   "idempotencyKey":  "optional-client-key",
     *   "expectedCurrency":"optional, e.g. EGP"
     * }
     * }</pre>
     *
     * @param request payment details
     * @return 201 Created with the resulting Payment record (status COMPLETED)
     */
    @PostMapping("/wallet/checkout")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentModuleApi.processPayment(
                request.bookingId(),
                request.customerId(),
                request.tenantId(),
                request.paymentAmount(),
                request.idempotencyKey(),
                request.expectedCurrency()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/payments/wallet/refund/{bookingId}
    // -------------------------------------------------------------------------

    /**
     * Refunds the wallet payment for a cancelled booking.
     *
     * <p>Expected request body:
     * <pre>{@code
     * {
     *   "bookingId":  "uuid",
     *   "customerId": "uuid",
     *   "tenantId":   "uuid"
     * }
     * }</pre>
     *
     * <p>The {@code bookingId} is also provided as a path variable for
     * RESTful resource identification. It must match the {@code bookingId}
     * in the body — a mismatch almost always signals a client bug (e.g. a
     * stale body reused against a different URL), so it is rejected instead
     * of silently trusting one value over the other.
     *
     * @param bookingId booking whose payment should be reversed (path variable)
     * @param request   refund context (bookingId, customerId, tenantId)
     * @return 200 OK with the updated Payment record (status REFUNDED)
     * @throws IllegalArgumentException if the path and body bookingId disagree
     */
    @PostMapping("/wallet/refund/{bookingId}")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RefundRequest request) {

        if (!bookingId.equals(request.bookingId())) {
            throw new IllegalArgumentException(
                    "bookingId in the path (%s) does not match bookingId in the request body (%s)"
                            .formatted(bookingId, request.bookingId()));
        }

        PaymentResponse response = paymentModuleApi.refundPayment(
                bookingId,
                request.customerId(),
                request.tenantId()
        );
        return ResponseEntity.ok(response);
    }
}