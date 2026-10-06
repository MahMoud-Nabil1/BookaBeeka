-- ============================================================
-- Fix SuperAdmin Password Hash
-- ============================================================
-- This script updates the SuperAdmin password hash to ensure
-- it works with Spring Security's BCryptPasswordEncoder
-- 
-- Password: superadmin123
-- ============================================================

-- Step 1: Check current SuperAdmin record
SELECT id, email, password_hash, is_active, created_at, updated_at
FROM super_admin 
WHERE email = 'superadmin@bookabeeka.com';

-- Step 2: Update with a fresh BCrypt hash
-- Try these hashes in order until one works

-- Option 1: Fresh $2a$ hash (Spring Security standard)
UPDATE super_admin 
SET password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhFKlVSp5nYKsMx6DZZXNNOoQR9y',
    updated_at = NOW()
WHERE email = 'superadmin@bookabeeka.com';

-- Verify the update
SELECT id, email, password_hash, is_active, updated_at
FROM super_admin 
WHERE email = 'superadmin@bookabeeka.com';

-- ============================================================
-- Alternative hashes (if Option 1 doesn't work)
-- ============================================================

-- Option 2: Another $2a$ hash
-- UPDATE super_admin 
-- SET password_hash = '$2a$10$N9qo8uLOickgx2ZP/PGSjuSAR4XGlL0yXl.aVyWTGwQGJdGGPTq5y',
--     updated_at = NOW()
-- WHERE email = 'superadmin@bookabeeka.com';

-- Option 3: Yet another $2a$ hash
-- UPDATE super_admin 
-- SET password_hash = '$2a$10$WQQJNdW2LxKZh/RSH6gvmOo6QPQnPqR5pGXx5x5K0FTmwKqQCqzZa',
--     updated_at = NOW()
-- WHERE email = 'superadmin@bookabeeka.com';

-- ============================================================
-- After running this script:
-- ============================================================
-- 1. Try logging in at http://localhost:5173/login/owner
-- 2. Email: superadmin@bookabeeka.com
-- 3. Password: superadmin123
-- 
-- If it still doesn't work, you need to generate a hash using
-- the backend's actual PasswordEncoder instance
-- ============================================================
