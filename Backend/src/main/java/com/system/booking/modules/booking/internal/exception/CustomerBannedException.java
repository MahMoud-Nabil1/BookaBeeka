package com.system.booking.modules.booking.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a customer whose account is banned attempts to create a booking.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CustomerBannedException extends RuntimeException {
    public CustomerBannedException(String message) {
        super(message);
    }
}
