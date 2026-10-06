-- V7: Add room_id to review table
-- Links a review directly to the room (resource) it is about,
-- so reviews can be queried by room even when service_id is null.

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS room_id UUID;

UPDATE review r
    SET room_id = b.resource_id
    FROM booking b
    WHERE r.booking_id = b.id
      AND r.room_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_review_room_id ON review (room_id);
