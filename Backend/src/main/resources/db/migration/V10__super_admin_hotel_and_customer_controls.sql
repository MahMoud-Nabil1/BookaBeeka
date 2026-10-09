-- ============================================================================
-- V10: Super Admin Platform Controls (Hotel & Customer Management)
-- ============================================================================

-- 1. Tenant suspension metadata
ALTER TABLE tenant
    ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS suspended_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS suspended_reason TEXT;

-- 2. Customer ban metadata
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS banned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS banned_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ban_reason TEXT;

-- 3. Indexes for fast filtering on status & ban flags
CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant(status);
CREATE INDEX IF NOT EXISTS idx_customer_banned ON customer(banned);
