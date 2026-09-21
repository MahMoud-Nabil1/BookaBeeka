-- Booking Schema: Hotel Specialization Columns
-- This is a MANUAL migration script. Run it against the database after deployment.
-- The project does NOT use Flyway — this file is for documentation and manual execution.
--
-- Adds hotel-specific columns to the booking table:
--   special_requests  : free-text guest requests
--   number_of_rooms   : quantity of rooms in this booking (default 1)
--
-- Note: resource_id is already the room reference (renamed to roomId in Java layer only).
--       check_in / check_out already exist from the original schema.

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS special_requests TEXT,
    ADD COLUMN IF NOT EXISTS number_of_rooms   INTEGER NOT NULL DEFAULT 1
        CONSTRAINT chk_number_of_rooms_positive CHECK (number_of_rooms >= 1);

-- Index to support typical "find active bookings for a room" queries
CREATE INDEX IF NOT EXISTS idx_booking_room_status
    ON booking (resource_id, status)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED');
