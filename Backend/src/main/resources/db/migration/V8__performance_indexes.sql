-- =============================================================
-- V8: Performance Indexes
-- =============================================================
-- These indexes target real query patterns identified in repository
-- and service code. All use IF NOT EXISTS for idempotency.
-- Apply manually on your Neon branch first, then production.
--
-- NOTE: Tables are small/early-stage → plain CREATE INDEX (not
-- CONCURRENTLY) is acceptable here. Switch to CONCURRENTLY once
-- the app goes live with real load.
-- =============================================================

-- ── resource ──────────────────────────────────────────────────
-- Serves: ResourceRepository.findByTenantId, countByTenantId
CREATE INDEX IF NOT EXISTS idx_resource_tenant
    ON resource (tenant_id);

-- Serves: ResourceRepository.findByTenantIdAndRoomTypeId
CREATE INDEX IF NOT EXISTS idx_resource_tenant_roomtype
    ON resource (tenant_id, room_type_id);

-- ── amenity ───────────────────────────────────────────────────
-- Serves: AmenityRepository.findByTenantId, existsByTenantIdAndName
CREATE INDEX IF NOT EXISTS idx_amenity_tenant
    ON amenity (tenant_id);

-- ── service (service_offering) ────────────────────────────────
-- Serves: ServiceOfferingRepository.findByTenantId, countByTenantId
CREATE INDEX IF NOT EXISTS idx_service_tenant
    ON service (tenant_id);

-- ── room_type ─────────────────────────────────────────────────
-- Serves: RoomTypeRepository.findByTenantId, existsByTenantIdAndName
CREATE INDEX IF NOT EXISTS idx_room_type_tenant
    ON room_type (tenant_id);

-- ── resource_amenity ──────────────────────────────────────────
-- Serves: ResourceAmenityRepository.findByResourceId (FK not auto-indexed in Postgres)
CREATE INDEX IF NOT EXISTS idx_resource_amenity_resource
    ON resource_amenity (resource_id);

-- Serves: ResourceAmenityRepository.findByTenantIdAndResourceId join on amenity_id
CREATE INDEX IF NOT EXISTS idx_resource_amenity_amenity
    ON resource_amenity (amenity_id);

-- ── resource_service_link ─────────────────────────────────────
-- Serves: ResourceServiceLinkRepository.findByResourceId
CREATE INDEX IF NOT EXISTS idx_resource_service_link_resource
    ON resource_service_link (resource_id);

-- Serves: ResourceServiceLinkRepository.findByServiceOfferingId
CREATE INDEX IF NOT EXISTS idx_resource_service_link_service
    ON resource_service_link (service_offering_id);

-- ── booking ───────────────────────────────────────────────────
-- Serves: BookingRepository.countByTenantId, findStuckBookings (SuperAdmin)
CREATE INDEX IF NOT EXISTS idx_booking_tenant
    ON booking (tenant_id);

-- Serves: BookingRepository.findByCustomerIdAndTenantId (customer's own bookings)
CREATE INDEX IF NOT EXISTS idx_booking_customer
    ON booking (customer_id);

-- Serves: BookingRepository.countByTenantIdAndStatus (SuperAdmin tenant detail)
CREATE INDEX IF NOT EXISTS idx_booking_tenant_status
    ON booking (tenant_id, status);

-- Serves: BookingRepository.findStuckBookings — filters by created_at cutoff
CREATE INDEX IF NOT EXISTS idx_booking_created_at
    ON booking (created_at);

-- ── payment ───────────────────────────────────────────────────
-- Serves: PaymentRepository.findByBookingIdOrderByCreatedAtDesc (FK not auto-indexed)
CREATE INDEX IF NOT EXISTS idx_payment_booking
    ON payment (booking_id);

-- Serves: PaymentRepository.findByStatusOrderByCreatedAtDesc (SuperAdmin failed payments)
CREATE INDEX IF NOT EXISTS idx_payment_status_created
    ON payment (status, created_at DESC);

-- ── wallet_transaction ────────────────────────────────────────
-- Serves: WalletTransactionRepository.findByWalletIdOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_wallet_transaction_wallet
    ON wallet_transaction (wallet_id);

-- Serves: WalletTransactionRepository join on booking_id (FK not auto-indexed)
CREATE INDEX IF NOT EXISTS idx_wallet_transaction_booking
    ON wallet_transaction (booking_id);

-- Serves: WalletTransactionRepository.findAllWithDetailsOrderByCreatedAtDesc (global sort)
CREATE INDEX IF NOT EXISTS idx_wallet_transaction_created
    ON wallet_transaction (created_at DESC);

-- ── notification ──────────────────────────────────────────────
-- Serves: NotificationRepository JPQL: WHERE tenant_id=? AND customer_id=?
CREATE INDEX IF NOT EXISTS idx_notification_tenant_customer
    ON notification (tenant_id, customer_id);

-- Serves: NotificationRepository.findRetryableNotifications: WHERE status=? AND retry_count<?
CREATE INDEX IF NOT EXISTS idx_notification_status_retry
    ON notification (status, retry_count);

-- ── hotel_admin ───────────────────────────────────────────────
-- Serves: OwnerReportingRepository.listTenantAdmins JOIN on tenant_id
CREATE INDEX IF NOT EXISTS idx_hotel_admin_tenant
    ON hotel_admin (tenant_id);

-- ── customer ──────────────────────────────────────────────────
-- Serves: CustomerRepository.findAllByOrderByCreatedAtDesc (SuperAdmin customer list)
CREATE INDEX IF NOT EXISTS idx_customer_created
    ON customer (created_at DESC);

-- ── tenant ────────────────────────────────────────────────────
-- Serves: TenantRepository.findAllByOrderByCreatedAtDesc (SuperAdmin tenant list)
CREATE INDEX IF NOT EXISTS idx_tenant_created
    ON tenant (created_at DESC);
