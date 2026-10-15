package com.system.booking.modules.payment.internal.listener;

import com.system.booking.modules.booking.internal.event.BookingCancelledEvent;
import com.system.booking.modules.payment.internal.service.WalletPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Domain event listener within the Payment module listening for {@link BookingCancelledEvent}.
 *
 * <p>Executes synchronously in the same database transaction as the booking cancellation.
 * Ensures the wallet refund, transaction ledger entry, and payment status update occur
 * atomically with the cancellation. If the refund fails, the transaction rolls back cleanly.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentBookingEventListener {

    private final WalletPaymentService walletPaymentService;

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("[PAYMENT_LISTENER] Received BookingCancelledEvent for bookingId=[{}], customerId=[{}], refundAmount=[{}], refundPct=[{}]",
                event.bookingId(), event.customerId(), event.refundAmount(), event.refundPercentage());

        walletPaymentService.processBookingRefund(
                event.bookingId(),
                event.customerId(),
                event.tenantId(),
                event.refundAmount(),
                event.refundPercentage()
        );
    }
}
