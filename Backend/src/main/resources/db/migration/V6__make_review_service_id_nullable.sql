-- V6: Make review.service_id nullable
--
-- Not every booking has a linked service offering (room-only bookings
-- with no ResourceServiceLink). The NOT NULL constraint was blocking
-- reviews for those bookings. service_id is kept for querying but is
-- now optional — reviews must not be gated on inventory completeness.

ALTER TABLE review
    ALTER COLUMN service_id DROP NOT NULL;
