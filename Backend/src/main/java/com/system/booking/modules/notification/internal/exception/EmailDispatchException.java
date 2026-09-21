package com.system.booking.modules.notification.internal.exception;

/**
 * Thrown when email dispatch through the mail server fails.
 */
public class EmailDispatchException extends RuntimeException {

    public EmailDispatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailDispatchException(String message) {
        super(message);
    }
}
