-- V5: Drop the foreign key constraint on review.service_id
--
-- Reviews are immutable historical records. The FK to the `service` table
-- (fkgwdirtrjebp7388pfmhblp1k1) was too strict:
--   - It blocks creating reviews when the booking's service_offering was deleted
--   - It blocks cascading service deletions even after all bookings are done
--
-- The service_id column is KEPT for querying (indexed below) — we just remove
-- the referential integrity constraint that was causing the FK violation errors.

ALTER TABLE review
    DROP CONSTRAINT IF EXISTS fkgwdirtrjebp7388pfmhblp1k1;
