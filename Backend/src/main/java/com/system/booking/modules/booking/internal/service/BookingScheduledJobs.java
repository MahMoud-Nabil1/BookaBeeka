package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

// background jobs that keep bookings in sync with reality
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduledJobs {

    private final BookingRepository bookingRepo;
    private final BookingLifecycleService lifecycleService;

    // every minute: expire pending bookings that sat too long without payment (10 min lock window)
    @Scheduled(fixedRate = 60_000)
    public void pendingBookingExpirySweep() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<Booking> stale = bookingRepo.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT, cutoff);

        for (Booking booking : stale) {
            try {
                lifecycleService.expireBooking(booking.getId());
            } catch (Exception e) {
                log.error("Failed to expire booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        if (!stale.isEmpty()) {
            log.info("Expired {} stale pending bookings", stale.size());
        }
    }

    // every hour: mark confirmed bookings as completed once their time slot ends
    @Scheduled(fixedRate = 3_600_000)
    public void bookingCompletionSweep() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Booking> done = bookingRepo.findByStatusAndEndTimeBefore(
                BookingStatus.CONFIRMED, now);

        for (Booking booking : done) {
            try {
                lifecycleService.completeBooking(booking.getId());
            } catch (Exception e) {
                log.error("Failed to complete booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        if (!done.isEmpty()) {
            log.info("Completed {} bookings whose slots have ended", done.size());
        }
    }
}
