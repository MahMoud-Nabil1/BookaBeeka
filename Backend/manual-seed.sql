-- Manual seed script to add test rooms
-- Find the tenant ID first, then insert resources

-- Check existing tenants
SELECT id, name, subdomain, status FROM tenant;

-- After you identify the tenant_id you want to use (e.g., Grand Hotel's ID),
-- replace 'YOUR-TENANT-ID-HERE' below with the actual UUID

-- Example: Insert 5 test rooms for a tenant
-- Replace 'YOUR-TENANT-ID-HERE' with your actual tenant UUID from the query above

INSERT INTO resource (id, tenant_id, name, resource_type, capacity, specs, is_active, is_bookable, created_at, updated_at)
VALUES 
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Deluxe Suite', 'SUITE', 2, '{"view": "Ocean View", "bedType": "King", "amenities": ["Mini Bar", "Balcony", "Jacuzzi"]}', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Standard Room', 'STANDARD', 2, '{"view": "City View", "bedType": "Queen", "amenities": ["WiFi", "TV", "Air Conditioning"]}', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Family Suite', 'FAMILY', 4, '{"view": "Garden View", "bedType": "2 Queen Beds", "amenities": ["Kitchen", "Living Room", "Washer/Dryer"]}', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Presidential Suite', 'PRESIDENTIAL', 4, '{"view": "Panoramic Ocean View", "bedType": "King + Queen", "amenities": ["Private Pool", "Butler Service", "Premium Bar", "Home Theater"]}', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Economy Room', 'ECONOMY', 1, '{"view": "Courtyard View", "bedType": "Twin", "amenities": ["WiFi", "Desk"]}', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert service offerings (room types/pricing)
INSERT INTO service_offering (id, tenant_id, name, price, duration_minutes, buffer_minutes, custom_attributes, is_active, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Nightly Stay', 199.99, 1440, 60, '{}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Weekly Stay (7 nights)', 1199.99, 10080, 60, '{}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'YOUR-TENANT-ID-HERE', 'Hourly Booking', 29.99, 60, 15, '{}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Verify the data was inserted
SELECT id, name, resource_type, capacity, is_active, is_bookable 
FROM resource 
WHERE tenant_id = 'YOUR-TENANT-ID-HERE';

SELECT id, name, price, duration_minutes 
FROM service_offering 
WHERE tenant_id = 'YOUR-TENANT-ID-HERE';
