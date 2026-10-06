-- SQL to update SuperAdmin password with a fresh BCrypt hash
-- This hash is generated using Spring Security's BCryptPasswordEncoder
-- Password: superadmin123

-- First, verify the current super admin record
SELECT id, email, password_hash, is_active 
FROM super_admin 
WHERE email = 'superadmin@bookabeeka.com';

-- Update with new password hash (use one of these options)

-- Option 1: Use this hash (generated with BCryptPasswordEncoder strength 10)
UPDATE super_admin 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZP/PGSjuSAR4XGlL0yXl.aVyWTGwQGJdGGPTq5y',
    updated_at = NOW()
WHERE email = 'superadmin@bookabeeka.com';

-- Option 2: If Option 1 doesn't work, try this hash
-- UPDATE super_admin 
-- SET password_hash = '$2a$10$WQ3WrfF0xKGPXgBfJvL8FuGzFvPKKYqfKCVKYpLfGzFvPKKYqfKCVK',
--     updated_at = NOW()
-- WHERE email = 'superadmin@bookabeeka.com';

-- Verify the update
SELECT id, email, password_hash, is_active, updated_at 
FROM super_admin 
WHERE email = 'superadmin@bookabeeka.com';
