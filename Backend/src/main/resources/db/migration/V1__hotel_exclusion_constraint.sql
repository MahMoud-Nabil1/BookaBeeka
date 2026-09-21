-- Hotel Availability: PostgreSQL exclusion constraint for double-booking prevention
-- This is a MANUAL migration script. Run it against the database after deployment.
-- The project does NOT use Flyway — this file is for documentation and manual execution.

-- Step 1: Enable btree_gist extension (required for UUID + daterange in GIST index)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Step 2: Exclusion constraint on the booking table
-- Prevents overlapping date-range bookings for the same room
-- Uses half-open range [check_in, check_out) so same-day checkout/checkin is allowed
-- Only PENDING_PAYMENT and CONFIRMED bookings participate
-- Legacy appointment bookings (NULL check_in/check_out) are excluded
ALTER TABLE booking ADD CONSTRAINT no_overlapping_room_bookings
    EXCLUDE USING GIST (
        resource_id WITH =,
        daterange(check_in, check_out) WITH &&
    )
    WHERE (
        status IN ('PENDING_PAYMENT', 'CONFIRMED')
        AND check_in IS NOT NULL
        AND check_out IS NOT NULL
    );

-- Step 3: Performance indexes for availability search queries
CREATE INDEX IF NOT EXISTS idx_booking_hotel_dates
    ON booking (resource_id, check_in, check_out)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED')
    AND check_in IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_room_blocks
    ON availability_exceptions (resource_id, start_date, end_date)
    WHERE is_available = false
    AND start_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_resource_bookable
    ON resource (tenant_id, is_active, is_bookable)
    WHERE is_active = true AND is_bookable = true;
