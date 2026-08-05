package com.system.booking.modules.booking.internal.exception;

// thrown when someone tries to book an already-taken slot
public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
