package com.system.booking.modules.payment.internal.exception;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thrown when a customer's wallet balance is insufficient to complete a payment.
 * Triggers a full transaction rollback to guarantee no partial state is persisted.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID customerId, BigDecimal required, BigDecimal available) {
        super(String.format(
                "Insufficient wallet balance for customer [%s]. Required: %s, Available: %s",
                customerId, required, available
        ));
    }
}
