package com.system.booking.modules.booking.internal.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

// maps booking exceptions to proper HTTP responses
@RestControllerAdvice
public class BookingExceptionHandler {

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleSlotUnavailable(SlotUnavailableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Slot unavailable",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(IllegalBookingStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleBadTransition(IllegalBookingStateTransitionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Invalid state transition",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Concurrent modification",
                "message", "Someone else modified this booking, try again",
                "timestamp", OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not found",
                "message", e.getMessage(),
                "timestamp", OffsetDateTime.now().toString()));
    }
}
