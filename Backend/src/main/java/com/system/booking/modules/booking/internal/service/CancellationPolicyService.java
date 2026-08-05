package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.CancellationPolicy;
import com.system.booking.modules.booking.internal.repository.CancellationPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// figures out refund amounts based on how far in advance you cancel
@Service
@RequiredArgsConstructor
public class CancellationPolicyService {

    private final CancellationPolicyRepository policyRepo;

    // walks the tiers from widest to narrowest, picks the first one that matches
    public int calculateRefundPercentage(Booking booking, OffsetDateTime cancelledAt) {
        List<CancellationPolicy> tiers = policyRepo
                .findByTenantIdOrderByHoursBeforeSlotDesc(booking.getTenantId());

        // no policy configured = full refund
        if (tiers.isEmpty()) {
            return 100;
        }

        long hoursUntilSlot = Duration.between(cancelledAt, booking.getStartTime()).toHours();

        for (CancellationPolicy tier : tiers) {
            if (hoursUntilSlot >= tier.getHoursBeforeSlot()) {
                return tier.getRefundPercentage();
            }
        }

        // past all tiers = no refund
        return 0;
    }

    // calculates the actual money amount from the percentage
    public BigDecimal calculateRefundAmount(Booking booking, OffsetDateTime cancelledAt) {
        int percentage = calculateRefundPercentage(booking, cancelledAt);
        return booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public List<CancellationPolicy> getPolicyForTenant(UUID tenantId) {
        return policyRepo.findByTenantIdOrderByHoursBeforeSlotDesc(tenantId);
    }
}
