package com.system.booking.modules.booking.internal.exception;

import com.system.booking.modules.booking.internal.entity.BookingStatus;

// thrown when the state machine rejects a transition
public class IllegalBookingStateTransitionException extends RuntimeException {

    public IllegalBookingStateTransitionException(BookingStatus from, BookingStatus to) {
        super("Can't move from " + from + " to " + to);
    }
}
