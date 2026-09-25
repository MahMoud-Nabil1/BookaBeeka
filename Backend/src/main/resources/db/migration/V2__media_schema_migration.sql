-- ============================================================
-- V2: Media Module Schema Migration
-- ============================================================
-- Purpose:
--   1. Drop obsolete NOT NULL columns from the old MediaPhoto skeleton
--      (entity_type, entity_id, url, display_order).
--      These are replaced by the resource_id FK and Cloudinary metadata.
--
--   2. Create partial unique index enforcing at most one primary photo
--      per resource at the database level.
--
-- Prerequisites:
--   - Run the app first so Hibernate ddl-auto=update creates the new
--     columns (resource_id, cloudinary_public_id, secure_url, etc.)
--   - Verify no existing data in media_photo before running.
--     If data exists, make columns nullable first and backfill.
--
-- Safety: Uses IF EXISTS / IF NOT EXISTS for idempotency.
-- ============================================================

-- Step 1: Verify no existing data (optional safety check).
-- If this returns rows, DO NOT proceed — backfill first.
-- SELECT count(*) FROM media_photo;

-- Step 2: Drop obsolete columns from the old skeleton schema.
-- These had NOT NULL constraints that would break new inserts.
ALTER TABLE media_photo DROP COLUMN IF EXISTS entity_type;
ALTER TABLE media_photo DROP COLUMN IF EXISTS entity_id;
ALTER TABLE media_photo DROP COLUMN IF EXISTS url;
ALTER TABLE media_photo DROP COLUMN IF EXISTS display_order;

-- Step 3: Partial unique index — at most one primary photo per resource.
CREATE UNIQUE INDEX IF NOT EXISTS idx_one_primary_per_resource
    ON media_photo (resource_id)
    WHERE is_primary = true;
