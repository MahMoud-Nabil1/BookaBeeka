package com.system.booking.modules.review.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReviewNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Review not found",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateReviewException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Duplicate review",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(BookingNotEligibleForReviewException.class)
    public ResponseEntity<Map<String, Object>> handleNotEligible(BookingNotEligibleForReviewException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Booking not eligible for review",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }

    // thrown by ReviewAdminController when SUPER_ADMIN omits the required tenantId
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Invalid request",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }
}
