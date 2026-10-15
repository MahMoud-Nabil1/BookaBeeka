-- V12: Booking Overlap Exclusion Constraint & Date-Range Index
-- ─────────────────────────────────────────────────────────────
-- This is a MANUAL migration script. Run it against the database after deployment.
-- The project does NOT use Flyway — this file is for documentation and manual execution.
--
-- Purpose:
--   1. Guarantee the GIST exclusion constraint that prevents double-booking is present.
--      (V1 defined this constraint but was also manual and may not have been applied.)
--   2. Add a composite index on (resource_id, check_in, check_out) to speed up the
--      NOT EXISTS correlated sub-query used in every availability search.
-- ─────────────────────────────────────────────────────────────

-- Step 1: btree_gist extension (required for UUID + daterange in GIST index)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Step 2: Add the exclusion constraint idempotently.
--   Uses a DO block so the script is safe to re-run on databases where V1 was already applied.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'no_overlapping_room_bookings'
          AND conrelid = 'booking'::regclass
    ) THEN
        ALTER TABLE booking ADD CONSTRAINT no_overlapping_room_bookings
            EXCLUDE USING GIST (
                resource_id WITH =,
                daterange(check_in, check_out) WITH &&
            )
            WHERE (
                status IN ('PENDING_PAYMENT', 'CONFIRMED')
                AND check_in  IS NOT NULL
                AND check_out IS NOT NULL
            );
    END IF;
END;
$$;

-- Step 3: Composite index for the NOT EXISTS date-overlap sub-query in availability search.
--   Covers: WHERE resource_id = ? AND status IN (...) AND check_in < ? AND check_out > ?
CREATE INDEX IF NOT EXISTS idx_booking_room_dates_active
    ON booking (resource_id, check_in, check_out)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED')
      AND check_in  IS NOT NULL
      AND check_out IS NOT NULL;
