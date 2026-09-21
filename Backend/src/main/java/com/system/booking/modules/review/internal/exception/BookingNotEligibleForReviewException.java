package com.system.booking.modules.review.internal.exception;

// thrown when the booking doesn't belong to the customer, or hasn't completed yet
public class BookingNotEligibleForReviewException extends RuntimeException {

    public BookingNotEligibleForReviewException(String message) {
        super(message);
    }
}
