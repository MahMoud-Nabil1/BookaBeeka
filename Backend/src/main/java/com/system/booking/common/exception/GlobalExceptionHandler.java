package com.system.booking.common.exception;

import com.system.booking.modules.payment.internal.exception.InsufficientBalanceException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralised exception → HTTP response mapping for the entire application.
 *
 * <p>Every handler returns a consistent JSON envelope so clients always know
 * what shape to expect on error:
 * <pre>{@code
 * {
 *   "timestamp": "2026-07-29T21:10:00",
 *   "status":    422,
 *   "error":     "Unprocessable Entity",
 *   "message":   "...",
 *   "path":      "/api/payments/wallet/checkout"
 * }
 * }</pre>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 400 Bad Request — Bean Validation failures (@Valid on request body)
    // -------------------------------------------------------------------------

    /**
     * Fires when a request body fails @NotNull / @Positive / etc. constraints.
     * Collects all violated fields into a single response so the client sees
     * every problem at once, not just the first one.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        List<String> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> "'" + fe.getField() + "': " + fe.getDefaultMessage())
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                String.join("; ", violations)
        );
    }

    // -------------------------------------------------------------------------
    // 402 Payment Required — insufficient wallet balance
    // -------------------------------------------------------------------------

    /**
     * Fires when {@code WalletPaymentService} determines the wallet balance is
     * too low. Returns 402 (Payment Required) — a semantically correct status
     * for a failed financial transaction.
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(
            InsufficientBalanceException ex) {

        return buildResponse(
                HttpStatus.PAYMENT_REQUIRED,
                "Insufficient wallet balance",
                ex.getMessage()
        );
    }

    // -------------------------------------------------------------------------
    // 404 Not Found — entity lookup failures
    // -------------------------------------------------------------------------

    /**
     * Fires when a wallet, payment, or booking cannot be found by ID.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage()
        );
    }

    // -------------------------------------------------------------------------
    // 422 Unprocessable Entity — business rule violations
    // -------------------------------------------------------------------------

    /**
     * Fires on service-layer guard failures (e.g. negative amount passed
     * programmatically, bypassing the controller validation layer).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid argument",
                ex.getMessage()
        );
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — unexpected failures
    // -------------------------------------------------------------------------

    /**
     * Catch-all for anything not handled above.
     * Does NOT expose the stack trace — only a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "Please contact support if this problem persists."
        );
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String error, String message) {

        // LinkedHashMap preserves insertion order in the serialized JSON
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status",    status.value());
        body.put("error",     error);
        body.put("message",   message);

        return ResponseEntity.status(status).body(body);
    }
}
