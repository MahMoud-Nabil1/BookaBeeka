package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.availability.api.AvailabilityModuleApi;
import com.system.booking.modules.booking.api.CancellationResultDto;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStateTransition;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.event.BookingCancelledEvent;
import com.system.booking.modules.booking.internal.event.BookingConfirmedEvent;
import com.system.booking.modules.booking.internal.event.BookingExpiredEvent;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.booking.internal.repository.BookingStateTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// manages everything that happens to a booking after it's created
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingLifecycleService {

    private final BookingRepository bookingRepo;
    private final BookingStateTransitionRepository transitionRepo;
    private final BookingStateMachine stateMachine;
    private final CancellationPolicyService cancellationPolicyService;
    private final AvailabilityModuleApi availabilityApi;
    private final ApplicationEventPublisher eventPublisher;

    // called when payment comes through
    @Transactional
    public void confirmBooking(UUID tenantId, UUID bookingId) {
        Booking booking = findBooking(tenantId, bookingId);
        stateMachine.assertTransitionAllowed(booking.getStatus(), BookingStatus.CONFIRMED);

        recordTransition(booking, BookingStatus.CONFIRMED, "Payment received", null);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking);

        eventPublisher.publishEvent(new BookingConfirmedEvent(
                booking.getId(), tenantId, booking.getCustomerId(),
                booking.getResourceId(), booking.getStartTime(), booking.getEndTime()));

        log.info("Booking {} confirmed", bookingId);
    }

    // called when a customer or admin cancels
    @Transactional
    public CancellationResultDto cancelBooking(UUID tenantId, UUID bookingId, String reason, UUID actorId) {
        Booking booking = findBooking(tenantId, bookingId);
        stateMachine.assertTransitionAllowed(booking.getStatus(), BookingStatus.CANCELLED);

        // only calculate refund if the customer actually paid
        int refundPercentage = 0;
        BigDecimal refundAmount = BigDecimal.ZERO;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            refundPercentage = cancellationPolicyService.calculateRefundPercentage(booking, OffsetDateTime.now());
            refundAmount = cancellationPolicyService.calculateRefundAmount(booking, OffsetDateTime.now());
        }

        recordTransition(booking, BookingStatus.CANCELLED, reason, actorId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        bookingRepo.save(booking);

        // free up the slot so others can book it
        if (booking.getLockId() != null) {
            try {
                availabilityApi.releaseLock(tenantId, booking.getLockId());
            } catch (Exception e) {
                log.warn("Couldn't release lock {} for booking {}: {}", booking.getLockId(), bookingId, e.getMessage());
            }
        }

        eventPublisher.publishEvent(new BookingCancelledEvent(
                booking.getId(), tenantId, refundAmount, reason));

        log.info("Booking {} cancelled, refund {}% ({})", bookingId, refundPercentage, refundAmount);

        return new CancellationResultDto(
                booking.getId(), refundAmount, refundPercentage, BookingStatus.CANCELLED.name());
    }

    // expires a pending booking that never got paid
    @Transactional
    public void expireBooking(UUID bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        stateMachine.assertTransitionAllowed(booking.getStatus(), BookingStatus.EXPIRED);

        recordTransition(booking, BookingStatus.EXPIRED, "Lock expired without payment", null);
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepo.save(booking);

        // release the slot lock if it's still there
        if (booking.getLockId() != null) {
            try {
                availabilityApi.releaseLock(booking.getTenantId(), booking.getLockId());
            } catch (Exception e) {
                log.warn("Lock {} already released for booking {}", booking.getLockId(), bookingId);
            }
        }

        eventPublisher.publishEvent(new BookingExpiredEvent(booking.getId(), booking.getTenantId()));
        log.info("Booking {} expired", bookingId);
    }

    // marks a booking as done once the slot time passes
    @Transactional
    public void completeBooking(UUID bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        stateMachine.assertTransitionAllowed(booking.getStatus(), BookingStatus.COMPLETED);

        recordTransition(booking, BookingStatus.COMPLETED, "Slot time passed", null);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepo.save(booking);

        log.info("Booking {} completed", bookingId);
    }

    // reschedule = cancel the old one + create a new one
    // the caller handles creating the new booking in the same transaction
    @Transactional
    public CancellationResultDto cancelForReschedule(UUID tenantId, UUID bookingId, UUID actorId) {
        return cancelBooking(tenantId, bookingId, "Rescheduled", actorId);
    }

    // saves a record of every status change for auditing
    private void recordTransition(Booking booking, BookingStatus to, String reason, UUID actorId) {
        BookingStateTransition transition = BookingStateTransition.builder()
                .bookingId(booking.getId())
                .fromStatus(booking.getStatus())
                .toStatus(to)
                .reason(reason)
                .actorId(actorId)
                .build();
        transitionRepo.save(transition);
    }

    private Booking findBooking(UUID tenantId, UUID bookingId) {
        return bookingRepo.findByTenantIdAndId(tenantId, bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
    }
}
