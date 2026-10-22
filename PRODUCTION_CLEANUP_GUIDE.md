# BookaBeeka — Production Readiness & Cleanup Guide

This document lists all unnecessary, temporary, scratch, and development-only files that should be cleaned up or excluded prior to deploying the project to production.

---

## 1. High Priority: Security & Sensitive Credentials

These files contain plain-text database credentials, API keys, or security test utilities and **must never be present in a production build or public repository**.

| File Path | Description | Recommended Action |
| :--- | :--- | :--- |
| `Backend/.env` | Contains live Neon DB credentials, Gmail SMTP app password, Cloudinary API secret, and JWT secret. | **Do NOT commit or deploy.** Ensure environment variables are injected via production hosting (e.g., Docker, AWS ECS, Render, Railway). |
| `Backend/generate-hash.sql` | SQL scratch script containing hardcoded hashes and test passwords. | **Delete** before release. |
| `Backend/fix-superadmin-password.sql` | SQL fix script with superadmin credentials. | **Delete** before release. |

---

## 2. Junk, Orphaned, and Misplaced Files

These files are completely unused, leftover from previous debugging sessions, or placed in incorrect source folders.

| File Path | Why It Can Be Removed | Recommended Action |
| :--- | :--- | :--- |
| `_tmp_bcrypt/` *(Entire folder)* | Temporary Node.js folder with `node_modules`, `package.json`, and `package-lock.json` created solely to test BCrypt hashing. | **Delete entire folder.** |
| `Backend/src/main/resources/BookaBeekaApplication.java` | Stray copy of `BookaBeekaApplication.java` placed inside `src/main/resources`. It gets packaged directly into the JAR root as a raw `.java` file. | **Delete immediately.** (The real class is in `src/main/java/...`). |
| `package-lock.json` *(at repository root)* | Empty dummy lockfile (`{"packages":{}}`) left at root. The actual frontend lockfile resides inside `FrontEnd/package-lock.json`. | **Delete.** |
| `Backend/src/test/java/com/system/booking/BCryptHashGeneratorTest.java` | Scratch unit test that only executes `System.out.println` with a hardcoded BCrypt string. | **Delete.** |

---

## 3. Developer Scratch Scripts & Test Files

Ad-hoc HTTP client files and PowerShell scripts used during local development to manually invoke or test endpoints.

| File Path | Purpose | Recommended Action |
| :--- | :--- | :--- |
| `test-owner-login.http` | HTTP scratch file for testing owner authentication. | **Delete** or move to `tests/http/`. |
| `Backend/clear-and-seed.http` | HTTP request to wipe and re-seed data. | **Delete**. |
| `Backend/debug-api.http` | HTTP debug requests. | **Delete**. |
| `Backend/test-api.http` | HTTP test collection for local dev. | **Delete**. |
| `Backend/test-search.http` | HTTP test collection for availability search. | **Delete**. |
| `Backend/trigger-seed.http` | HTTP request triggering seed endpoints. | **Delete**. |
| `diagnose-login.ps1` | PowerShell diagnostic script for testing user logins. | **Delete**. |
| `test-password-hash.ps1` | PowerShell script testing hash verification. | **Delete**. |
| `build_collection.ps1` | PowerShell helper script. | **Delete** or archive. |
| `Backend/clear-database.sql` | SQL script to truncate tables during dev resets. | **Delete** (dangerous in production). |
| `Backend/manual-seed.sql` | Manual SQL seed script with mock data. | **Delete** or move to `db/seeds/` if needed for dev onboarding. |

---

## 4. Temporary Planning & Notes Documents

Markdown notes created during development that are not user documentation.

| File Path | Description | Recommended Action |
| :--- | :--- | :--- |
| `FRONTEND_PLAN.md` | Temporary frontend development notes and task lists. | **Delete** or archive to a private wiki. |
| `verify-password.md` | Local scratch notes on password verification steps. | **Delete**. |

---

## 5. Development-Only Code & Controllers (Hardening)

These files are located in `src/main/java`, but should not be exposed in production:

| Class | Route / Purpose | Action Before Production |
| :--- | :--- | :--- |
| `DataSeederController.java` | `/api/dev/seed/**` (includes `/generate-hash` which generates BCrypt hashes on public GET requests). | Currently guarded by `@Profile("dev")`. For production, ensure the active profile is set to `prod` so this controller is never loaded, or delete before final build. |
| `PaymentSeedController.java` | `/api/payments/seed/**` (creates wallets without authentication). | Guarded by `@Profile({"dev", "test", "local"})`. Ensure `prod` profile does not load this bean. |

---

## 6. Recommended Root `.gitignore` Update

The root `.gitignore` currently only excludes IDEs and `.env`. To prevent future scratch files, build artifacts, and dependencies from polluting Git, add the following entries:

```gitignore
# IDEs and editors
.idea/
.vscode/
*.iml
*.iws
*.ipr

# OS metadata
.DS_Store
Thumbs.db

# Environment variables
.env
*.env
.env.local

# Temporary scratch folders
_tmp_bcrypt/
*.log
*.tmp

# Root orphan lockfiles
/package-lock.json

# Backend build outputs
Backend/target/
Backend/.mvn/wrapper/maven-wrapper.jar

# Frontend build outputs & dependencies
FrontEnd/node_modules/
FrontEnd/dist/
FrontEnd/dist-ssr/
```

---

## Quick One-Liner Cleanup Command (PowerShell)

To quickly remove all scratch, temporary, and misplaced files in one go (run from the repository root `A:\JAVA PROJECTS\BookingProject2\BookaBeeka`):

```powershell
# Remove temporary directory and misplaced files
Remove-Item -Recurse -Force "_tmp_bcrypt" -ErrorAction SilentlyContinue
Remove-Item -Force "package-lock.json" -ErrorAction SilentlyContinue
Remove-Item -Force "Backend\src\main\resources\BookaBeekaApplication.java" -ErrorAction SilentlyContinue

# Remove dev scripts and scratch HTTP files
Remove-Item -Force "diagnose-login.ps1", "test-password-hash.ps1", "build_collection.ps1" -ErrorAction SilentlyContinue
Remove-Item -Force "test-owner-login.http", "verify-password.md", "FRONTEND_PLAN.md" -ErrorAction SilentlyContinue
Remove-Item -Force "Backend\clear-and-seed.http", "Backend\debug-api.http", "Backend\test-api.http", "Backend\test-search.http", "Backend\trigger-seed.http" -ErrorAction SilentlyContinue
Remove-Item -Force "Backend\clear-database.sql", "Backend\fix-superadmin-password.sql", "Backend\generate-hash.sql", "Backend\manual-seed.sql" -ErrorAction SilentlyContinue
Remove-Item -Force "Backend\src\test\java\com\system\booking\BCryptHashGeneratorTest.java" -ErrorAction SilentlyContinue
```
