package com.system.booking.modules.booking.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an attempt is made to create a booking at a hotel that is suspended or inactive.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class HotelSuspendedException extends RuntimeException {
    public HotelSuspendedException(String message) {
        super(message);
    }
}
