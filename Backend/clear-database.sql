-- Clear all data from the database to allow fresh seeding
-- Run this script in your PostgreSQL database (neondb)
-- WARNING: This will delete ALL data!

BEGIN;

-- Delete in order respecting foreign key constraints
DELETE FROM wallet_transaction;
DELETE FROM tenant_wallet;
DELETE FROM notification;
DELETE FROM payment;
DELETE FROM booking;
DELETE FROM slot_locks;
DELETE FROM availability_exception;
DELETE FROM schedule_rule;
DELETE FROM resource_amenity_link;
DELETE FROM amenity;
DELETE FROM resource_service_link;
DELETE FROM resource;
DELETE FROM room_type;
DELETE FROM service_offering;
DELETE FROM customer;
DELETE FROM hotel_admin;
DELETE FROM owner;
DELETE FROM super_admin;
DELETE FROM tenant;

COMMIT;

-- Verify all tables are empty
SELECT 'tenant' as table_name, COUNT(*) as count FROM tenant
UNION ALL
SELECT 'resource', COUNT(*) FROM resource
UNION ALL
SELECT 'service_offering', COUNT(*) FROM service_offering
UNION ALL
SELECT 'owner', COUNT(*) FROM owner
UNION ALL
SELECT 'customer', COUNT(*) FROM customer;
