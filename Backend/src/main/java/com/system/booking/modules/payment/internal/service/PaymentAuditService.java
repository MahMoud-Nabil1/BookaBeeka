package com.system.booking.modules.payment.internal.service;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.PaymentStatus;
import com.system.booking.modules.payment.internal.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dedicated service for persisting audit records that must survive a transaction rollback.
 *
 * <p>When a checkout payment fails (insufficient balance), the outer
 * {@code @Transactional} in {@link WalletPaymentService} rolls back every write —
 * including the PENDING payment record. This means failed attempts are invisible in the DB.
 * <br>
 * This service uses {@code Propagation.REQUIRES_NEW}, which suspends the caller's
 * transaction, opens a brand-new one, commits it independently, then resumes the
 * caller. The FAILED record is therefore committed and visible even after the outer
 * rollback.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuditService {

    private final PaymentRepository paymentRepository;
    private final EntityManager     entityManager;

    /**
     * Persists a FAILED payment record in its own transaction.
     * Called after InsufficientBalanceException is thrown, before the outer transaction rolls back.
     *
     * @param bookingId     booking that was being paid for
     * @param tenantId      tenant context
     * @param amount        amount that was requested but could not be fulfilled
     * @param currency      wallet currency
     * @param failureReason human-readable reason for the failure
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedPayment(UUID bookingId, UUID tenantId,
                                    BigDecimal amount, String currency,
                                    String failureReason) {
        try {
            Booking bookingRef = entityManager.getReference(Booking.class, bookingId);

            Payment failed = Payment.builder()
                    .tenantId(tenantId)
                    .booking(bookingRef)
                    .amount(amount)
                    .currency(currency)
                    .status(PaymentStatus.FAILED)
                    .paymentMethod("WALLET")
                    .failureReason(failureReason)
                    .build();

            paymentRepository.save(failed);
            log.info("Audit: FAILED payment recorded for booking [{}]. Reason: {}", bookingId, failureReason);

        } catch (Exception e) {
            // Audit failure must NEVER prevent the InsufficientBalanceException
            // from propagating to the caller. Log and swallow.
            log.error("Audit: could not record failed payment for booking [{}]: {}", bookingId, e.getMessage());
        }
    }
}
