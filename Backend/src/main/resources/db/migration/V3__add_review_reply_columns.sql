-- Adds staff-reply support to the existing `review` table.
-- Rename this file to match your migration tool's naming convention
-- (e.g. Flyway: V{next_number}__add_review_reply_columns.sql)

ALTER TABLE review
    ADD COLUMN reply TEXT,
    ADD COLUMN replied_by UUID,
    ADD COLUMN replied_by_role VARCHAR(20),
    ADD COLUMN replied_at TIMESTAMP(6);

-- one review per booking
ALTER TABLE review
    ADD CONSTRAINT uq_review_booking_id UNIQUE (booking_id);

-- guard rating at the DB level too, not just in the DTO validation
ALTER TABLE review
    ADD CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5);

-- support the query patterns used by ReviewRepository
CREATE INDEX idx_review_tenant_id ON review (tenant_id);
CREATE INDEX idx_review_service_id ON review (service_id);
CREATE INDEX idx_review_customer_id ON review (customer_id);
