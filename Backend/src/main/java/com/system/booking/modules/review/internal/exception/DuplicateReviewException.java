package com.system.booking.modules.review.internal.exception;

import java.util.UUID;

public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(UUID bookingId) {
        super("A review already exists for booking: " + bookingId);
    }
}
