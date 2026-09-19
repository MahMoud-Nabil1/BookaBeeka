-- ============================================================
-- V4: Seed the platform SuperAdmin account
-- ============================================================
-- Credentials:
--   email    : superadmin@bookabeeka.com
--   password : superadmin123   (BCrypt strength-10 hash below)
--
-- Change the password immediately after first login in production.
-- ============================================================

INSERT INTO super_admin (id, email, password_hash, first_name, last_name, is_active, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'superadmin@bookabeeka.com',
    '$2b$10$rxCoBwmZ9HVLGNCKKeWuG.cE/FTLUq9q0JqHJhcMKM262STgRoL2m',
    'Super',
    'Admin',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM super_admin WHERE email = 'superadmin@bookabeeka.com'
);
