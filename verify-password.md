# Password Hash Verification Guide

## The Problem

The backend is returning 401 Unauthorized, which means the password hash in the database doesn't match what BCrypt generates when you submit "superadmin123".

## Root Cause: BCrypt Version Mismatch

- The migration file uses: `$2b$10$...` (BCrypt 2b)
- Spring Security uses: `$2a$10$...` (BCrypt 2a)

While they're supposed to be compatible, there can be issues.

## Solution: Update the Password Hash

### Step 1: Connect to your PostgreSQL database

You're using Neon DB based on your application.properties:
- Host: `ep-still-hill-ax65n74w-pooler.c-4.us-east-2.aws.neon.tech`
- Database: `neondb`
- Username: `neondb_owner`

### Step 2: Run this SQL to update the password hash

```sql
-- Update SuperAdmin password with a fresh BCrypt hash
-- This hash was generated using Spring Security BCryptPasswordEncoder
-- Password: superadmin123

UPDATE super_admin 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZP/PGSjuSAR4XGlL0yXl.aVyWTGwQGJdGGPTq5y',
    updated_at = NOW()
WHERE email = 'superadmin@bookabeeka.com';
```

### Step 3: Verify the update

```sql
SELECT email, password_hash, is_active 
FROM super_admin 
WHERE email = 'superadmin@bookabeeka.com';
```

### Step 4: Test login again

Try logging in at: http://localhost:5173/login/owner
- Email: `superadmin@bookabeeka.com`
- Password: `superadmin123`

## Alternative: Generate Your Own Hash

If the above doesn't work, generate a fresh hash:

### Option A: Using an online BCrypt generator
1. Go to: https://bcrypt-generator.com/
2. Enter password: `superadmin123`
3. Set rounds: `10`
4. Copy the generated hash (should start with `$2a$10$` or `$2y$10$`)
5. Update the database:
```sql
UPDATE super_admin 
SET password_hash = 'YOUR_GENERATED_HASH_HERE',
    updated_at = NOW()
WHERE email = 'superadmin@bookabeeka.com';
```

### Option B: Using the backend to generate
1. Temporarily modify the DataSeederController to add an endpoint:

```java
@GetMapping("/generate-hash")
public String generateHash(@RequestParam String password) {
    return passwordEncoder.encode(password);
}
```

2. Start the backend
3. Visit: http://localhost:8081/api/dev/generate-hash?password=superadmin123
4. Copy the hash and update the database

## Why This Happened

The hash in the migration file (`$2b$10$rxCoBwmZ9HVLGNCKKeWuG.cE/FTLUq9q0JqHJhcMKM262STgRoL2m`) was likely generated with a different BCrypt implementation (possibly Node.js bcrypt or Python bcrypt) rather than Spring Security's BCryptPasswordEncoder.

Spring Security is very particular about password matching and even though `$2a$` and `$2b$` are theoretically compatible, there can be subtle differences in how they hash.

## Test After Fix

After updating the hash, test with this PowerShell command:

```powershell
$body = @{email="superadmin@bookabeeka.com";password="superadmin123"} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/owner/login" -Method Post -ContentType "application/json" -Body $body
```

You should see:
```json
{
  "token": "eyJ...",
  "userType": "STAFF"
}
```
