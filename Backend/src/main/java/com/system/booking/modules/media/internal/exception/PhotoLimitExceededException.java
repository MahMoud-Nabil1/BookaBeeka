package com.system.booking.modules.media.internal.exception;

public class PhotoLimitExceededException extends RuntimeException {
    public PhotoLimitExceededException(String message) {
        super(message);
    }
}
