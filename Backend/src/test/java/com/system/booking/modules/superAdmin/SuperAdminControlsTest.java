package com.system.booking.modules.superAdmin;

import com.system.booking.common.config.CacheConfig;
import com.system.booking.modules.availability.api.AvailabilityModuleApi;
import com.system.booking.modules.availability.internal.service.CatalogVersionService;
import com.system.booking.modules.booking.api.CreateBookingRequestDto;
import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.booking.internal.entity.BookingStatus;
import com.system.booking.modules.booking.internal.exception.CustomerBannedException;
import com.system.booking.modules.booking.internal.exception.HotelSuspendedException;
import com.system.booking.modules.booking.internal.repository.BookingRepository;
import com.system.booking.modules.booking.internal.service.BookingCreationService;
import com.system.booking.modules.booking.internal.service.IdempotencyService;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.hoteladmin.internal.repository.HotelAdminRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.media.internal.repository.MediaPhotoRepository;
import com.system.booking.modules.media.internal.service.CloudinaryService;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import com.system.booking.modules.payment.internal.repository.CustomerWalletRepository;
import com.system.booking.modules.payment.internal.repository.PaymentRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.payment.internal.repository.WalletTransactionRepository;
import com.system.booking.modules.superAdmin.api.dto.CustomerSummaryResponse;
import com.system.booking.modules.superAdmin.api.dto.TenantSummaryResponse;
import com.system.booking.modules.superAdmin.internal.repository.SuperAdminRepository;
import com.system.booking.modules.superAdmin.internal.service.SuperAdminService;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminControlsTest {

    @Nested
    @DisplayName("SuperAdminService Hotel & Customer Control Tests")
    class SuperAdminServiceTests {

        @Mock private SuperAdminRepository superAdminRepository;
        @Mock private TenantRepository tenantRepository;
        @Mock private CustomerRepository customerRepository;
        @Mock private BookingRepository bookingRepository;
        @Mock private PaymentRepository paymentRepository;
        @Mock private WalletTransactionRepository walletTransactionRepository;
        @Mock private CustomerWalletRepository customerWalletRepository;
        @Mock private TenantWalletRepository tenantWalletRepository;
        @Mock private ResourceRepository resourceRepository;
        @Mock private OwnerRepository ownerRepository;
        @Mock private HotelAdminRepository hotelAdminRepository;
        @Mock private MediaPhotoRepository mediaPhotoRepository;
        @Mock private NotificationRepository notificationRepository;
        @Mock private CatalogVersionService catalogVersionService;
        @Mock private CloudinaryService cloudinaryService;
        @Mock private CacheManager cacheManager;
        @Mock private EntityManager entityManager;

        @InjectMocks
        private SuperAdminService superAdminService;

        private UUID tenantId;
        private Tenant testTenant;
        private UUID customerId;
        private Customer testCustomer;

        @BeforeEach
        void setUp() {
            tenantId = UUID.randomUUID();
            testTenant = Tenant.builder()
                    .name("Grand Hotel")
                    .subdomain("grand-hotel")
                    .status("ACTIVE")
                    .currency("USD")
                    .timezone("UTC")
                    .build();
            testTenant.setId(tenantId);

            customerId = UUID.randomUUID();
            testCustomer = Customer.builder()
                    .email("john@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .passwordHash("hashed")
                    .isActive(true)
                    .banned(false)
                    .build();
            testCustomer.setId(customerId);
        }

        @Test
        @DisplayName("listTenants calculates batch active owners and admins count correctly")
        void listTenants_calculatesBatchActiveOwnerAndAdminCounts() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Tenant> page = new PageImpl<>(List.of(testTenant), pageable, 1);

            when(tenantRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);
            List<Object[]> ownerCounts = new ArrayList<>();
            ownerCounts.add(new Object[]{tenantId, 2L});
            when(ownerRepository.countActiveByTenantIds(List.of(tenantId))).thenReturn(ownerCounts);

            List<Object[]> adminCounts = new ArrayList<>();
            adminCounts.add(new Object[]{tenantId, 4L});
            when(hotelAdminRepository.countActiveByTenantIds(List.of(tenantId))).thenReturn(adminCounts);

            Page<TenantSummaryResponse> result = superAdminService.listTenants(pageable);

            assertEquals(1, result.getTotalElements());
            TenantSummaryResponse summary = result.getContent().get(0);
            assertEquals("Grand Hotel", summary.name());
            assertEquals(2L, summary.ownersCount());
            assertEquals(4L, summary.adminsCount());
            verify(ownerRepository).countActiveByTenantIds(List.of(tenantId));
            verify(hotelAdminRepository).countActiveByTenantIds(List.of(tenantId));
        }

        @Test
        @DisplayName("suspendTenant updates status to SUSPENDED, sets audit metadata, evicts cache and increments catalog version")
        void suspendTenant_success() {
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
            when(ownerRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(1L);
            when(hotelAdminRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(2L);
            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache(anyString())).thenReturn(mockCache);

            TenantSummaryResponse response = superAdminService.suspendTenant(tenantId, "Policy violation", "admin@super.com");

            assertEquals("SUSPENDED", testTenant.getStatus());
            assertEquals("admin@super.com", testTenant.getSuspendedBy());
            assertEquals("Policy violation", testTenant.getSuspendedReason());
            assertNotNull(testTenant.getSuspendedAt());

            assertEquals("SUSPENDED", response.status());
            assertEquals("Policy violation", response.suspendedReason());
            verify(tenantRepository).save(testTenant);
            verify(catalogVersionService).increment();
            verify(cacheManager, atLeastOnce()).getCache(CacheConfig.CACHE_TENANTS);
            verify(cacheManager, atLeastOnce()).getCache(CacheConfig.CACHE_ROOM_TYPES);
        }

        @Test
        @DisplayName("unsuspendTenant restores status to ACTIVE, clears audit fields, evicts cache and increments catalog version")
        void unsuspendTenant_success() {
            testTenant.setStatus("SUSPENDED");
            testTenant.setSuspendedReason("Temp pause");
            testTenant.setSuspendedBy("admin@super.com");
            testTenant.setSuspendedAt(LocalDateTime.now().minusDays(1));

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
            when(ownerRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(1L);
            when(hotelAdminRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(2L);
            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache(anyString())).thenReturn(mockCache);

            TenantSummaryResponse response = superAdminService.unsuspendTenant(tenantId, "superadmin");

            assertEquals("ACTIVE", testTenant.getStatus());
            assertNull(testTenant.getSuspendedBy());
            assertNull(testTenant.getSuspendedReason());
            assertNull(testTenant.getSuspendedAt());

            assertEquals("ACTIVE", response.status());
            assertNull(response.suspendedReason());
            verify(tenantRepository).save(testTenant);
            verify(catalogVersionService).increment();
        }

        @Test
        @DisplayName("deleteTenant throws IllegalStateException when active or upcoming bookings exist")
        void deleteTenant_blockedWhenActiveBookingsExist() {
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
            when(bookingRepository.countActiveOrUpcomingBookingsByTenant(eq(tenantId), any(LocalDate.class), any(OffsetDateTime.class)))
                    .thenReturn(3L);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    superAdminService.deleteTenant(tenantId, "superadmin"));

            assertTrue(ex.getMessage().contains("Cannot delete hotel with 3 active or upcoming booking(s)"));
            verify(tenantRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteTenant cascades child deletions and cleans up when 0 active bookings")
        void deleteTenant_successWhenNoActiveBookings() {
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(testTenant));
            when(bookingRepository.countActiveOrUpcomingBookingsByTenant(eq(tenantId), any(LocalDate.class), any(OffsetDateTime.class)))
                    .thenReturn(0L);
            when(mediaPhotoRepository.findByTenantId(tenantId)).thenReturn(Collections.emptyList());

            Query mockQuery = mock(Query.class);
            when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.executeUpdate()).thenReturn(1);

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache(anyString())).thenReturn(mockCache);

            superAdminService.deleteTenant(tenantId, "superadmin");

            verify(mediaPhotoRepository).deleteByTenantId(tenantId);
            verify(tenantRepository).delete(testTenant);
            verify(catalogVersionService).increment();
            verify(cacheManager, atLeastOnce()).getCache(CacheConfig.CACHE_TENANTS);
        }

        @Test
        @DisplayName("banCustomer sets banned status and audit metadata")
        void banCustomer_success() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

            CustomerSummaryResponse response = superAdminService.banCustomer(customerId, "Chargeback fraud", "admin@super.com");

            assertTrue(testCustomer.getBanned());
            assertEquals("Chargeback fraud", testCustomer.getBanReason());
            assertEquals("admin@super.com", testCustomer.getBannedBy());
            assertNotNull(testCustomer.getBannedAt());

            assertTrue(response.banned());
            assertEquals("Chargeback fraud", response.banReason());
            verify(customerRepository).save(testCustomer);
        }

        @Test
        @DisplayName("unbanCustomer clears banned status and audit metadata")
        void unbanCustomer_success() {
            testCustomer.setBanned(true);
            testCustomer.setBanReason("Chargeback fraud");
            testCustomer.setBannedBy("admin@super.com");
            testCustomer.setBannedAt(LocalDateTime.now().minusDays(2));

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

            CustomerSummaryResponse response = superAdminService.unbanCustomer(customerId, "superadmin");

            assertFalse(testCustomer.getBanned());
            assertNull(testCustomer.getBanReason());
            assertNull(testCustomer.getBannedBy());
            assertNull(testCustomer.getBannedAt());

            assertFalse(response.banned());
            verify(customerRepository).save(testCustomer);
        }

        @Test
        @DisplayName("deleteCustomer throws IllegalStateException when active or upcoming booking exists")
        void deleteCustomer_blockedWhenActiveBookingsExist() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

            LocalDate futureCheckout = LocalDate.now().plusDays(3);
            Booking activeBooking = Booking.builder()
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .checkOut(futureCheckout)
                    .status(BookingStatus.CONFIRMED)
                    .build();

            when(bookingRepository.findActiveOrUpcomingBookingsByCustomer(eq(customerId), any(LocalDate.class), any(OffsetDateTime.class)))
                    .thenReturn(List.of(activeBooking));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    superAdminService.deleteCustomer(customerId, "superadmin"));

            assertTrue(ex.getMessage().contains("Customer has an active or upcoming booking until " + futureCheckout));
            verify(customerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteCustomer deletes notifications, favorites, wallet and customer record when no active bookings")
        void deleteCustomer_successWhenNoActiveBookings() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
            when(bookingRepository.findActiveOrUpcomingBookingsByCustomer(eq(customerId), any(LocalDate.class), any(OffsetDateTime.class)))
                    .thenReturn(Collections.emptyList());

            Query mockQuery = mock(Query.class);
            when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.executeUpdate()).thenReturn(1);

            CustomerWallet wallet = CustomerWallet.builder()
                    .customer(testCustomer)
                    .balance(BigDecimal.ZERO)
                    .currency("USD")
                    .build();
            wallet.setId(UUID.randomUUID());
            when(customerWalletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(wallet));

            superAdminService.deleteCustomer(customerId, "superadmin");

            verify(notificationRepository).deleteByCustomerId(customerId);
            verify(walletTransactionRepository).deleteByWalletId(wallet.getId());
            verify(customerWalletRepository).delete(wallet);
            verify(customerRepository).delete(testCustomer);
        }
    }

    @Nested
    @DisplayName("Booking Creation Restrictions Tests")
    class BookingCreationRestrictionsTests {

        @Mock private BookingRepository bookingRepo;
        @Mock private AvailabilityModuleApi availabilityApi;
        @Mock private InventoryModuleApi inventoryApi;
        @Mock private IdempotencyService idempotencyService;
        @Mock private ApplicationEventPublisher eventPublisher;
        @Mock private CustomerRepository customerRepo;
        @Mock private TenantRepository tenantRepo;

        @InjectMocks
        private BookingCreationService bookingCreationService;

        private UUID tenantId;
        private UUID customerId;
        private CreateBookingRequestDto request;

        @BeforeEach
        void setUp() {
            tenantId = UUID.randomUUID();
            customerId = UUID.randomUUID();
            request = new CreateBookingRequestDto(
                    tenantId,
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(3),
                    1,
                    "Quiet room please",
                    Map.of()
            );
        }

        @Test
        @DisplayName("createBooking throws CustomerBannedException (HTTP 403) when customer is banned")
        void createBooking_throwsCustomerBannedExceptionWhenBanned() {
            when(idempotencyService.begin(any(), any())).thenReturn(Optional.empty());

            Customer bannedCustomer = Customer.builder()
                    .email("banned@user.com")
                    .firstName("Banned")
                    .lastName("User")
                    .banned(true)
                    .banReason("Repeated fraudulent activity")
                    .build();
            bannedCustomer.setId(customerId);

            when(customerRepo.findById(customerId)).thenReturn(Optional.of(bannedCustomer));

            CustomerBannedException ex = assertThrows(CustomerBannedException.class, () ->
                    bookingCreationService.createBooking(request, customerId, "idem-key-1"));

            assertTrue(ex.getMessage().contains("banned from making bookings"));
            assertTrue(ex.getMessage().contains("Repeated fraudulent activity"));
            verify(tenantRepo, never()).findById(any());
            verify(bookingRepo, never()).save(any());
        }

        @Test
        @DisplayName("createBooking throws HotelSuspendedException (HTTP 409) when hotel is suspended")
        void createBooking_throwsHotelSuspendedExceptionWhenHotelSuspended() {
            when(idempotencyService.begin(any(), any())).thenReturn(Optional.empty());

            Customer activeCustomer = Customer.builder()
                    .email("good@user.com")
                    .firstName("Good")
                    .lastName("User")
                    .banned(false)
                    .build();
            activeCustomer.setId(customerId);
            when(customerRepo.findById(customerId)).thenReturn(Optional.of(activeCustomer));

            Tenant suspendedTenant = Tenant.builder()
                    .name("Suspended Resort")
                    .subdomain("suspended-resort")
                    .status("SUSPENDED")
                    .build();
            suspendedTenant.setId(tenantId);
            when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(suspendedTenant));

            HotelSuspendedException ex = assertThrows(HotelSuspendedException.class, () ->
                    bookingCreationService.createBooking(request, customerId, "idem-key-2"));

            assertTrue(ex.getMessage().contains("suspended or unavailable for bookings"));
            verify(bookingRepo, never()).save(any());
        }
    }
}
