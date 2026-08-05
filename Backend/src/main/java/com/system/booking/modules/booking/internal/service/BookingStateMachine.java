package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.exception.IllegalBookingStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

// enforces the booking lifecycle rules so nothing weird can happen
@Component
public class BookingStateMachine {

    // the only transitions that are allowed
    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(
            BookingStatus.PENDING_PAYMENT, Set.of(BookingStatus.CONFIRMED, BookingStatus.EXPIRED, BookingStatus.CANCELLED),
            BookingStatus.CONFIRMED, Set.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED),
            BookingStatus.CANCELLED, Set.of(),
            BookingStatus.COMPLETED, Set.of(),
            BookingStatus.EXPIRED, Set.of()
    );

    // blows up if the transition isn't legal
    public void assertTransitionAllowed(BookingStatus from, BookingStatus to) {
        Set<BookingStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalBookingStateTransitionException(from, to);
        }
    }

    // returns what states you can move to from the current one
    public Set<BookingStatus> nextValidStates(BookingStatus current) {
        return TRANSITIONS.getOrDefault(current, Set.of());
    }
}
