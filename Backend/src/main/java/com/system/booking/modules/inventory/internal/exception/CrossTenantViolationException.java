package com.system.booking.modules.inventory.internal.exception;

public class CrossTenantViolationException extends RuntimeException {

    public CrossTenantViolationException(String message) {
        super(message);
    }
}