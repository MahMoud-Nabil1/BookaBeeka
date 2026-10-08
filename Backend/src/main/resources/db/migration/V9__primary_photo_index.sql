-- V9: Partial index on media_photo for primary photo batch query
-- Added to support the batch photo fetch in RoomAvailabilityRepository.findPrimaryPhotosByRoomIds()
-- which filters WHERE is_primary = true. Without this index, that query scans all photo rows.

CREATE INDEX IF NOT EXISTS idx_media_photo_is_primary
    ON media_photo (resource_id)
    WHERE is_primary = true;

COMMENT ON INDEX idx_media_photo_is_primary IS
    'Supports batch primary-photo lookup in availability search: WHERE resource_id IN (...) AND is_primary = true';
