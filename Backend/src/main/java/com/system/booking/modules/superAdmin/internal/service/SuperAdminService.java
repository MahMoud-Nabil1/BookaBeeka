package com.system.booking.modules.superAdmin.internal.service;

import com.system.booking.common.config.CacheConfig;
import com.system.booking.config.SystemTenantInitializer;
import com.system.booking.modules.availability.internal.service.CatalogVersionService;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.hoteladmin.internal.repository.HotelAdminRepository;
import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.media.internal.entity.MediaPhoto;
import com.system.booking.modules.media.internal.repository.MediaPhotoRepository;
import com.system.booking.modules.media.internal.service.CloudinaryService;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import com.system.booking.modules.payment.internal.entity.Payment;
import com.system.booking.modules.payment.internal.entity.PaymentStatus;
import com.system.booking.modules.payment.internal.entity.TenantWallet;
import com.system.booking.modules.payment.internal.entity.WalletTransaction;
import com.system.booking.modules.payment.internal.repository.CustomerWalletRepository;
import com.system.booking.modules.payment.internal.repository.PaymentRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.payment.internal.repository.WalletTransactionRepository;
import com.system.booking.modules.superAdmin.api.dto.*;
import com.system.booking.modules.superAdmin.internal.repository.SuperAdminRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final SuperAdminRepository       superAdminRepository;
    private final TenantRepository           tenantRepository;
    private final CustomerRepository         customerRepository;
    private final BookingRepository          bookingRepository;
    private final PaymentRepository          paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CustomerWalletRepository    customerWalletRepository;
    private final TenantWalletRepository      tenantWalletRepository;
    private final ResourceRepository          resourceRepository;
    private final OwnerRepository             ownerRepository;
    private final HotelAdminRepository        hotelAdminRepository;
    private final MediaPhotoRepository        mediaPhotoRepository;
    private final NotificationRepository      notificationRepository;
    private final CatalogVersionService       catalogVersionService;
    private final CloudinaryService           cloudinaryService;
    private final CacheManager                cacheManager;
    private final EntityManager               entityManager;

    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        long totalTenants     = tenantRepository.count();
        long activeTenants    = tenantRepository.countByStatus("ACTIVE");
        long suspendedTenants = tenantRepository.countByStatus("SUSPENDED");
        long bannedTenants    = tenantRepository.countByStatus("BANNED");

        long totalCustomers  = customerRepository.count();
        long bannedCustomers = customerRepository.countByBannedTrue();

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
        Page<Tenant> page = tenantRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<UUID> tenantIds = page.getContent().stream().map(Tenant::getId).toList();

        Map<UUID, Long> ownersMap = new HashMap<>();
        Map<UUID, Long> adminsMap = new HashMap<>();

        if (!tenantIds.isEmpty()) {
            for (Object[] row : ownerRepository.countActiveByTenantIds(tenantIds)) {
                ownersMap.put((UUID) row[0], ((Number) row[1]).longValue());
            }
            for (Object[] row : hotelAdminRepository.countActiveByTenantIds(tenantIds)) {
                adminsMap.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        return page.map(t -> toTenantSummary(
                t,
                ownersMap.getOrDefault(t.getId(), 0L),
                adminsMap.getOrDefault(t.getId(), 0L)
        ));
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

        long ownersCount = ownerRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long adminsCount = hotelAdminRepository.countByTenantIdAndIsActiveTrue(tenantId);

        return new TenantDetailResponse(
                tenant.getId(), tenant.getName(), tenant.getSubdomain(),
                tenant.getStatus(), tenant.getCurrency(), tenant.getTimezone(),
                revenueBalance,
                totalBookings, confirmedBookings, completedBookings, cancelledBookings,
                ownersCount, adminsCount,
                tenant.getSuspendedAt(), tenant.getSuspendedBy(), tenant.getSuspendedReason(),
                tenant.getCreatedAt()
        );
    }

    @Transactional
    public TenantSummaryResponse updateTenantStatus(UUID tenantId, String newStatus) {
        if (!newStatus.equals("ACTIVE") && !newStatus.equals("SUSPENDED") && !newStatus.equals("BANNED")) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Must be ACTIVE, SUSPENDED, or BANNED.");
        }

        if ("SUSPENDED".equals(newStatus)) {
            return suspendTenant(tenantId, null, "SUPER_ADMIN");
        } else if ("ACTIVE".equals(newStatus)) {
            return unsuspendTenant(tenantId, "SUPER_ADMIN");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        tenant.setStatus(newStatus);
        tenantRepository.save(tenant);
        evictTenantCaches(tenant.getId(), tenant.getSubdomain());
        catalogVersionService.increment();

        long ownersCount = ownerRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long adminsCount = hotelAdminRepository.countByTenantIdAndIsActiveTrue(tenantId);
        return toTenantSummary(tenant, ownersCount, adminsCount);
    }

    @Transactional
    public TenantSummaryResponse suspendTenant(UUID tenantId, String reason, String actor) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        tenant.setStatus("SUSPENDED");
        tenant.setSuspendedAt(LocalDateTime.now());
        tenant.setSuspendedBy(sanitizeActor(actor));
        tenant.setSuspendedReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        tenantRepository.save(tenant);

        evictTenantCaches(tenant.getId(), tenant.getSubdomain());
        catalogVersionService.increment();

        log.info("SuperAdmin suspended tenant: actor={}, tenantId={}, reason={}", actor, tenantId, reason);

        long ownersCount = ownerRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long adminsCount = hotelAdminRepository.countByTenantIdAndIsActiveTrue(tenantId);
        return toTenantSummary(tenant, ownersCount, adminsCount);
    }

    @Transactional
    public TenantSummaryResponse unsuspendTenant(UUID tenantId, String actor) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        tenant.setStatus("ACTIVE");
        tenant.setSuspendedAt(null);
        tenant.setSuspendedBy(null);
        tenant.setSuspendedReason(null);
        tenantRepository.save(tenant);

        evictTenantCaches(tenant.getId(), tenant.getSubdomain());
        catalogVersionService.increment();

        log.info("SuperAdmin unsuspended tenant: actor={}, tenantId={}", actor, tenantId);

        long ownersCount = ownerRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long adminsCount = hotelAdminRepository.countByTenantIdAndIsActiveTrue(tenantId);
        return toTenantSummary(tenant, ownersCount, adminsCount);
    }

    @Transactional
    public void deleteTenant(UUID tenantId, String actor) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        // Safety default: block deletion if active or upcoming bookings exist
        LocalDate today = LocalDate.now();
        OffsetDateTime now = OffsetDateTime.now();
        long activeBookings = bookingRepository.countActiveOrUpcomingBookingsByTenant(tenantId, today, now);
        if (activeBookings > 0) {
            throw new IllegalStateException("Cannot delete hotel with " + activeBookings +
                    " active or upcoming booking(s). Please resolve or cancel them first.");
        }

        // Collect Cloudinary public IDs for post-commit image cleanup
        List<MediaPhoto> photos = mediaPhotoRepository.findByTenantId(tenantId);
        List<String> cloudinaryIds = photos.stream()
                .map(MediaPhoto::getCloudinaryPublicId)
                .filter(Objects::nonNull)
                .toList();

        // Cascade delete child entities inside this transaction using native SQL in FK dependency order:
        // 1. Availability exceptions / room blocks
        entityManager.createNativeQuery("DELETE FROM availability_exceptions WHERE tenant_id = :tId OR resource_id IN (SELECT id FROM resource WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();

        // 2. Schedule rules
        entityManager.createNativeQuery("DELETE FROM schedule_rules WHERE tenant_id = :tId OR resource_id IN (SELECT id FROM resource WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();

        // 3. Slot locks
        entityManager.createNativeQuery("DELETE FROM slot_locks WHERE tenant_id = :tId OR resource_id IN (SELECT id FROM resource WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();

        // 4. Media photos
        mediaPhotoRepository.deleteByTenantId(tenantId);

        // 5. Customer favorites
        entityManager.createNativeQuery("DELETE FROM customer_favorite WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 6. Reviews (must be deleted before booking & resources because review.booking_id & review.room_id)
        entityManager.createNativeQuery("DELETE FROM review WHERE tenant_id = :tId OR booking_id IN (SELECT id FROM booking WHERE tenant_id = :tId) OR room_id IN (SELECT id FROM resource WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();

        // 7. Notification booking unlinking & notification deletion for this tenant
        entityManager.createNativeQuery("UPDATE notification SET booking_id = NULL WHERE booking_id IN (SELECT id FROM booking WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM notification WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 8. Child state transitions referencing bookings
        entityManager.createNativeQuery("DELETE FROM booking_state_transition WHERE booking_id IN (SELECT id FROM booking WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();

        // 9. Unlink customer wallet ledger entries from this tenant's bookings & payments
        entityManager.createNativeQuery("""
                UPDATE wallet_transaction
                SET booking_id = NULL, payment_id = NULL
                WHERE booking_id IN (SELECT id FROM booking WHERE tenant_id = :tId)
                   OR payment_id IN (SELECT id FROM payment WHERE tenant_id = :tId OR booking_id IN (SELECT id FROM booking WHERE tenant_id = :tId))
                """)
                .setParameter("tId", tenantId).executeUpdate();

        // 10. Financial records (payment) for this tenant or its bookings
        entityManager.createNativeQuery("""
                DELETE FROM payment
                WHERE tenant_id = :tId
                   OR booking_id IN (SELECT id FROM booking WHERE tenant_id = :tId)
                """)
                .setParameter("tId", tenantId).executeUpdate();

        // 11. Idempotency keys for this tenant
        entityManager.createNativeQuery("DELETE FROM \"key\" WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 12. Bookings for this tenant (MUST be deleted before resources because booking.resource_id -> resource.id)
        entityManager.createNativeQuery("DELETE FROM booking WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 13. Resource amenities & resource service links
        entityManager.createNativeQuery("DELETE FROM resource_amenity WHERE resource_id IN (SELECT id FROM resource WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM resource_service_link WHERE resource_id IN (SELECT id FROM resource WHERE tenant_id = :tId)")
                .setParameter("tId", tenantId).executeUpdate();

        // 14. Resources (rooms) - now safe because booking.resource_id references were deleted above
        entityManager.createNativeQuery("DELETE FROM resource WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 15. Room types - now safe because resource references were deleted above
        entityManager.createNativeQuery("DELETE FROM room_type WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 16. Amenities
        entityManager.createNativeQuery("DELETE FROM amenity WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 17. Services (table name is 'service')
        entityManager.createNativeQuery("DELETE FROM service WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 18. Attribute schemas
        entityManager.createNativeQuery("DELETE FROM attribute_schema WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 19. Cancellation policy
        entityManager.createNativeQuery("DELETE FROM cancellation_policy WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 20. Owners & Admins
        entityManager.createNativeQuery("DELETE FROM hotel_owner WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM hotel_admin WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 21. Tenant wallet
        entityManager.createNativeQuery("DELETE FROM tenant_wallet WHERE tenant_id = :tId")
                .setParameter("tId", tenantId).executeUpdate();

        // 22. Legacy tables cleanup (if branch exists from earlier schema versions)
        try {
            Number branchCount = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'branch'")
                    .getSingleResult();
            if (branchCount != null && branchCount.intValue() > 0) {
                entityManager.createNativeQuery("DELETE FROM branch WHERE tenant_id = :tId")
                        .setParameter("tId", tenantId).executeUpdate();
            }
        } catch (Exception ex) {
            log.debug("Legacy branch cleanup check skipped: {}", ex.getMessage());
        }

        // 23. Tenant entity
        tenantRepository.delete(tenant);

        // Evict caches and bump catalog version
        evictTenantCaches(tenantId, tenant.getSubdomain());
        catalogVersionService.increment();

        log.info("SuperAdmin deleted tenant: actor={}, tenantId={}, name={}", actor, tenantId, tenant.getName());

        // Best-effort non-blocking Cloudinary image deletion
        cleanupCloudinaryPhotosBestEffort(cloudinaryIds);
    }

    private void cleanupCloudinaryPhotosBestEffort(List<String> cloudinaryIds) {
        if (cloudinaryIds == null || cloudinaryIds.isEmpty()) return;
        new Thread(() -> {
            for (String publicId : cloudinaryIds) {
                try {
                    cloudinaryService.deleteAsset(publicId);
                } catch (Exception e) {
                    log.warn("Best-effort Cloudinary deletion failed for {}: {}", publicId, e.getMessage());
                }
            }
        }).start();
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> listCustomers(Pageable pageable) {
        return customerRepository.findAllExcludingSystem(SystemTenantInitializer.ANONYMOUS_CUSTOMER_ID, pageable)
                .map(this::toCustomerSummary);
    }

    @Transactional
    public CustomerSummaryResponse banCustomer(UUID customerId) {
        return banCustomer(customerId, null, "SUPER_ADMIN");
    }

    @Transactional
    public CustomerSummaryResponse banCustomer(UUID customerId, String reason, String actor) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        customer.setBanned(true);
        customer.setBannedAt(LocalDateTime.now());
        customer.setBannedBy(sanitizeActor(actor));
        customer.setBanReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        customerRepository.save(customer);

        log.info("SuperAdmin banned customer: actor={}, customerId={}, reason={}", actor, customerId, reason);
        return toCustomerSummary(customer);
    }

    @Transactional
    public CustomerSummaryResponse unbanCustomer(UUID customerId) {
        return unbanCustomer(customerId, "SUPER_ADMIN");
    }

    @Transactional
    public CustomerSummaryResponse unbanCustomer(UUID customerId, String actor) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        customer.setBanned(false);
        customer.setBannedAt(null);
        customer.setBannedBy(null);
        customer.setBanReason(null);
        customerRepository.save(customer);

        log.info("SuperAdmin unbanned customer: actor={}, customerId={}", actor, customerId);
        return toCustomerSummary(customer);
    }

    @Transactional
    public void deleteCustomer(UUID customerId, String actor) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        LocalDate today = LocalDate.now();
        OffsetDateTime now = OffsetDateTime.now();
        List<Booking> activeBookings = bookingRepository.findActiveOrUpcomingBookingsByCustomer(customerId, today, now);
        if (!activeBookings.isEmpty()) {
            Booking b = activeBookings.get(0);
            LocalDate activeUntil = b.getCheckOut() != null ? b.getCheckOut() : b.getEndTime().toLocalDate();
            throw new IllegalStateException("Customer has an active or upcoming booking until " + activeUntil);
        }

        // 1. Delete notifications
        notificationRepository.deleteByCustomerId(customerId);

        // 2. Delete customer favorites
        entityManager.createNativeQuery("DELETE FROM customer_favorite WHERE customer_id = :cId")
                .setParameter("cId", customerId).executeUpdate();

        // 3. Delete wallet and ledger entries
        customerWalletRepository.findByCustomerId(customerId).ifPresent(wallet -> {
            walletTransactionRepository.deleteByWalletId(wallet.getId());
            customerWalletRepository.delete(wallet);
        });

        // 4. Ensure platform anonymous customer exists for historical booking & review preservation
        ensureAnonymousCustomerExists();

        // 5. Anonymize/reassign historical bookings & reviews to the platform anonymous user
        // so financial accounting, revenue totals, and ratings remain mathematically intact.
        entityManager.createNativeQuery("UPDATE booking SET customer_id = :anonId WHERE customer_id = :cId")
                .setParameter("anonId", SystemTenantInitializer.ANONYMOUS_CUSTOMER_ID)
                .setParameter("cId", customerId)
                .executeUpdate();

        entityManager.createNativeQuery("UPDATE review SET customer_id = :anonId WHERE customer_id = :cId")
                .setParameter("anonId", SystemTenantInitializer.ANONYMOUS_CUSTOMER_ID)
                .setParameter("cId", customerId)
                .executeUpdate();

        // 6. Delete customer row
        customerRepository.delete(customer);

        log.info("SuperAdmin deleted customer: actor={}, customerId={}, email={}", actor, customerId, customer.getEmail());
    }

    private void ensureAnonymousCustomerExists() {
        entityManager.createNativeQuery("""
            INSERT INTO customer (id, email, password_hash, first_name, last_name, is_active, banned, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-000000000002', 'deleted-user@platform.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi50jvd77Gq5gD0eP/84.XfQW809nku', 'Deleted', 'Customer', FALSE, TRUE, NOW(), NOW())
            ON CONFLICT (id) DO NOTHING
        """).executeUpdate();
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

    @Transactional(readOnly = true)
    public List<ResourceResponse> listRoomsByTenant(UUID tenantId) {
        return resourceRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toResourceResponse)
                .toList();
    }

    private void evictTenantCaches(UUID tenantId, String subdomain) {
        evictCache(CacheConfig.CACHE_TENANTS, tenantId);
        if (subdomain != null) {
            evictCache(CacheConfig.CACHE_TENANT_SUBDOMAIN, subdomain);
        }
        clearCache(CacheConfig.CACHE_ROOM_TYPES);
        clearCache(CacheConfig.CACHE_RESOURCES);
        clearCache(CacheConfig.CACHE_RESOURCE);
        clearCache(CacheConfig.CACHE_AMENITIES);
        clearCache(CacheConfig.CACHE_SERVICE_OFFERINGS);
        clearCache(CacheConfig.CACHE_OWNER_DASHBOARD);
        clearCache(CacheConfig.CACHE_OWNER_REVENUE);
    }

    private void evictCache(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private TenantSummaryResponse toTenantSummary(Tenant t, long ownersCount, long adminsCount) {
        return new TenantSummaryResponse(
                t.getId(), t.getName(), t.getSubdomain(),
                t.getStatus(), t.getCurrency(), t.getTimezone(),
                ownersCount, adminsCount,
                t.getSuspendedAt(), t.getSuspendedBy(), t.getSuspendedReason(),
                t.getCreatedAt()
        );
    }

    private CustomerSummaryResponse toCustomerSummary(Customer c) {
        var walletBalance = customerWalletRepository.findByCustomerId(c.getId())
                .map(CustomerWallet::getBalance)
                .orElse(null);
        return new CustomerSummaryResponse(
                c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(),
                c.getPhone(), c.getIsActive(),
                c.getBanned(), c.getBannedAt(), c.getBannedBy(), c.getBanReason(),
                walletBalance, c.getCreatedAt()
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

    private ResourceResponse toResourceResponse(Resource r) {
        return new ResourceResponse(
                r.getId(),
                r.getTenantId(),
                r.getRoomType() != null ? r.getRoomType().getId() : null,
                r.getRoomType() != null ? r.getRoomType().getName() : null,
                r.getName(),
                r.getRoomNumber(),
                r.getFloor(),
                r.getStatus(),
                r.getResourceType(),
                r.getCapacity(),
                r.getSpecs(),
                r.getIsActive(),
                r.getIsBookable(),
                r.getPricePerNight(),
                r.getCurrency(),
                r.getCreatedAt()
        );
    }

    private String sanitizeActor(String actor) {
        if (actor == null || actor.isBlank()) return "SUPER_ADMIN";
        return actor.length() > 100 ? actor.substring(0, 100) : actor.trim();
    }
}