package com.system.booking.modules.superAdmin.internal.service;

import com.system.booking.modules.superAdmin.api.dto.*;
import com.system.booking.modules.superAdmin.internal.entity.SuperAdmin;
import com.system.booking.modules.superAdmin.internal.repository.SuperAdminRepository;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.PaymentStatus;
import com.system.booking.modules.payment.internal.entity.TenantWallet;
import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import com.system.booking.modules.payment.internal.repository.CustomerWalletRepository;
import com.system.booking.modules.payment.internal.repository.PaymentRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.payment.internal.repository.WalletTransactionRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepository;
    private final TenantRepository     tenantRepository;
    private final CustomerRepository   customerRepository;
    private final BookingRepository    bookingRepository;
    private final PaymentRepository    paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CustomerWalletRepository    customerWalletRepository;
    private final TenantWalletRepository      tenantWalletRepository;

    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        long totalTenants     = tenantRepository.count();
        long activeTenants    = tenantRepository.countByStatus("ACTIVE");
        long suspendedTenants = tenantRepository.countByStatus("SUSPENDED");
        long bannedTenants    = tenantRepository.countByStatus("BANNED");

        long totalCustomers  = customerRepository.count();
        long bannedCustomers = customerRepository.countByIsActiveFalse();

        long totalHotelUsers = 0L;

        long totalBookings     = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);

        LocalDateTime stuckCutoff = LocalDateTime.now().minusMinutes(30);
        long stuckBookings = bookingRepository.findStuckBookings(
                BookingStatus.PENDING_PAYMENT, stuckCutoff, Pageable.unpaged()).getTotalElements();

        long failedPayments = paymentRepository.countByStatus(PaymentStatus.FAILED);

        var platformRevenue    = tenantWalletRepository.sumAllBalances();
        var moneyInCirculation = customerWalletRepository.sumAllBalances();

        return new PlatformStatsResponse(
                totalTenants, activeTenants, suspendedTenants, bannedTenants,
                totalCustomers, bannedCustomers,
                totalHotelUsers,
                totalBookings, confirmedBookings, completedBookings, cancelledBookings, stuckBookings,
                failedPayments,
                platformRevenue, moneyInCirculation
        );
    }

    @Transactional(readOnly = true)
    public Page<TenantSummaryResponse> listTenants(Pageable pageable) {
        return tenantRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toTenantSummary);
    }

    @Transactional(readOnly = true)
    public TenantDetailResponse getTenantDetail(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        var revenueBalance = tenantWalletRepository.findByTenantId(tenantId)
                .map(TenantWallet::getBalance)
                .orElse(java.math.BigDecimal.ZERO);

        long totalBookings     = bookingRepository.countByTenantId(tenantId);
        long confirmedBookings = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.CONFIRMED);
        long completedBookings = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.COMPLETED);
        long cancelledBookings = bookingRepository.countByTenantIdAndStatus(tenantId, BookingStatus.CANCELLED);

        return new TenantDetailResponse(
                tenant.getId(), tenant.getName(), tenant.getSubdomain(),
                tenant.getStatus(), tenant.getCurrency(), tenant.getTimezone(),
                revenueBalance,
                totalBookings, confirmedBookings, completedBookings, cancelledBookings,
                tenant.getCreatedAt()
        );
    }

    @Transactional
    public TenantSummaryResponse updateTenantStatus(UUID tenantId, String newStatus) {
        if (!newStatus.equals("ACTIVE") && !newStatus.equals("SUSPENDED") && !newStatus.equals("BANNED")) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Must be ACTIVE, SUSPENDED, or BANNED.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        tenant.setStatus(newStatus);
        tenantRepository.save(tenant);
        return toTenantSummary(tenant);
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> listCustomers(Pageable pageable) {
        return customerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toCustomerSummary);
    }

    @Transactional
    public CustomerSummaryResponse banCustomer(UUID customerId) {
        return setCustomerActive(customerId, false);
    }

    @Transactional
    public CustomerSummaryResponse unbanCustomer(UUID customerId) {
        return setCustomerActive(customerId, true);
    }

    @Transactional(readOnly = true)
    public Page<PlatformTransactionResponse> listTransactions(Pageable pageable) {
        return walletTransactionRepository.findAllWithDetailsOrderByCreatedAtDesc(pageable)
                .map(this::toPlatformTransaction);
    }

    @Transactional(readOnly = true)
    public Page<FailedPaymentResponse> listFailedPayments(Pageable pageable) {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED, pageable)
                .map(this::toFailedPayment);
    }

    @Transactional(readOnly = true)
    public Page<StuckBookingResponse> listStuckBookings(int thresholdMinutes, Pageable pageable) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(thresholdMinutes);
        return bookingRepository.findStuckBookings(BookingStatus.PENDING_PAYMENT, cutoff, pageable)
                .map(b -> toStuckBooking(b, thresholdMinutes));
    }

    @Transactional(readOnly = true)
    public Page<TenantWallet> listTenantWallets(Pageable pageable) {
        return tenantWalletRepository.findAllByOrderByBalanceDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<CustomerWalletSummaryResponse> listCustomerWallets(Pageable pageable) {
        return customerWalletRepository.findAllByOrderByBalanceDesc(pageable)
                .map(this::toCustomerWalletSummary);
    }

    private TenantSummaryResponse toTenantSummary(Tenant t) {
        return new TenantSummaryResponse(
                t.getId(), t.getName(), t.getSubdomain(),
                t.getStatus(), t.getCurrency(), t.getTimezone(),
                t.getCreatedAt()
        );
    }

    private CustomerSummaryResponse toCustomerSummary(Customer c) {
        var walletBalance = customerWalletRepository.findByCustomerId(c.getId())
                .map(w -> w.getBalance())
                .orElse(null);
        return new CustomerSummaryResponse(
                c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(),
                c.getPhone(), c.getIsActive(), walletBalance, c.getCreatedAt()
        );
    }

    private PlatformTransactionResponse toPlatformTransaction(WalletTransaction wt) {
        var customer = wt.getWallet().getCustomer();
        var booking  = wt.getBooking();
        var payment  = wt.getPayment();

        return new PlatformTransactionResponse(
                wt.getId(),
                wt.getTransactionType().name(),
                wt.getAmount(),
                wt.getBalanceAfter(),
                customer.getId(),
                customer.getEmail(),
                customer.getFirstName() + " " + customer.getLastName(),
                booking != null ? booking.getTenantId() : null,
                booking != null ? booking.getId()       : null,
                payment != null ? payment.getId()       : null,
                wt.getDescription(),
                wt.getCreatedAt()
        );
    }

    private FailedPaymentResponse toFailedPayment(Payment p) {
        return new FailedPaymentResponse(
                p.getId(),
                p.getBooking().getId(),
                p.getBooking().getTenantId(),
                p.getAmount(),
                p.getCurrency(),
                p.getFailureReason(),
                p.getCreatedAt()
        );
    }

    private StuckBookingResponse toStuckBooking(Booking b, int thresholdMinutes) {
        long minutesStuck = ChronoUnit.MINUTES.between(b.getCreatedAt(), LocalDateTime.now());
        return new StuckBookingResponse(
                b.getId(),
                b.getCustomerId(),
                b.getTenantId(),
                b.getTotalAmount(),
                b.getCurrency(),
                minutesStuck,
                b.getCreatedAt()
        );
    }

    private CustomerWalletSummaryResponse toCustomerWalletSummary(CustomerWallet w) {
        return new CustomerWalletSummaryResponse(
                w.getId(),
                w.getCustomer().getId(),
                w.getCustomer().getEmail(),
                w.getBalance(),
                w.getCurrency(),
                w.getUpdatedAt()
        );
    }

    private CustomerSummaryResponse setCustomerActive(UUID customerId, boolean active) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        customer.setIsActive(active);
        customerRepository.save(customer);
        return toCustomerSummary(customer);
    }
}