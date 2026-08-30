# SuperAdmin Module — Implementation Plan (APPROVED)

## Decisions
- **Role strings:** `SUPER_ADMIN`, `OWNER`, `ADMIN`
- **SuperAdmin auth:** Option B — separate `super_admin` table + own login endpoint, independent from Staff

---

## Phase 1 — Ground

### 1.1 StaffRole Enum
#### [NEW] `modules/staff/internal/entity/StaffRole.java`
Values: `OWNER`, `ADMIN` (SUPER_ADMIN is NOT a staff role)

---

### 1.2 Customer.isActive
#### [MODIFY] `modules/customer/internal/entity/Customer.java`
Add: `Boolean isActive = true`
**DB migration:** `ALTER TABLE customer ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;`

---

### 1.3 SuperAdmin Entity & Auth
#### [NEW] `modules/security/entity/SuperAdmin.java`
Fields: email, passwordHash, firstName, lastName, isActive

#### [NEW] `modules/security/repository/SuperAdminRepository.java`
`findByEmail(String email)`

#### [NEW] `modules/security/dto/SuperAdminAuthDTO.java`
Record: id, email, passwordHash, isActive

#### [NEW] `modules/security/port/in/SuperAdminAuthenticationPort.java`
`findSuperAdminByEmail(String email)`

#### [NEW] `modules/security/service/SuperAdminSecurityAdapter.java`
Implements `SuperAdminAuthenticationPort`

#### [MODIFY] `modules/security/service/JwtService.java`
- Add `generateSuperAdminToken(SuperAdminAuthDTO)` — JWT carries `user_type=SUPER_ADMIN`, no tenant_id
- Add parsing methods: `extractSubject`, `extractClaim`, `extractAllClaims`, `isTokenExpired`

#### [MODIFY] `modules/security/service/AuthenticationService.java`
Add `authenticateSuperAdmin(SuperAdminLoginRequest)`

#### [NEW] `modules/security/dto/request/SuperAdminLoginRequest.java`

#### [MODIFY] `modules/security/controller/AuthenticationController.java`
Add `POST /auth/super-admin/login`

---

### 1.4 JWT Validation Pipeline
#### [NEW] `modules/security/config/JwtAuthenticationFilter.java`
`extends OncePerRequestFilter` — reads Bearer token, populates SecurityContext:
- `user_type=SUPER_ADMIN` → authority `ROLE_SUPER_ADMIN`
- `user_type=STAFF`, `role=OWNER` → authority `ROLE_OWNER`
- `user_type=STAFF`, `role=ADMIN` → authority `ROLE_ADMIN`
- `user_type=CUSTOMER` → authority `ROLE_CUSTOMER`

#### [MODIFY] `modules/security/config/SecurityConfig.java`
- Add `@EnableMethodSecurity`
- Wire `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- Open `/auth/**` and `/customers/register`

---

## Phase 2 — Missing Repositories

#### [NEW] `modules/tenant/internal/repository/TenantRepository.java`
#### [NEW] `modules/review/internal/repository/ReviewRepository.java`
#### [NEW] `modules/notification/internal/repository/NotificationRepository.java`
#### [MODIFY] `modules/payment/internal/repository/WalletTransactionRepository.java` — add platform-wide paginated query
#### [MODIFY] `modules/payment/internal/repository/PaymentRepository.java` — add `findByStatus` paginated

---

## Phase 3 — SuperAdmin Module

```
modules/admin/
├── api/
│   ├── SuperAdminController.java
│   └── dto/
│       ├── PlatformStatsResponse.java
│       ├── TenantSummaryResponse.java
│       ├── TenantDetailResponse.java
│       ├── CustomerSummaryResponse.java
│       ├── PlatformTransactionResponse.java
│       ├── FailedPaymentResponse.java
│       ├── StuckBookingResponse.java
│       └── ReviewModerationResponse.java
└── internal/service/
    └── SuperAdminService.java
```

### Endpoints (all `@PreAuthorize("hasRole('SUPER_ADMIN')")`)
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/super/stats` | Platform KPIs |
| GET | `/api/admin/super/tenants` | Paginated tenant list |
| GET | `/api/admin/super/tenants/{id}` | Tenant detail |
| PATCH | `/api/admin/super/tenants/{id}/status` | Activate/Suspend/Ban tenant |
| GET | `/api/admin/super/customers` | Paginated customer list |
| PATCH | `/api/admin/super/customers/{id}/ban` | Ban customer |
| PATCH | `/api/admin/super/customers/{id}/unban` | Unban customer |
| GET | `/api/admin/super/transactions` | Platform-wide transaction feed |
| GET | `/api/admin/super/payments/failed` | All FAILED payments |
| GET | `/api/admin/super/bookings/stuck` | PENDING_PAYMENT > 30 min |
| GET | `/api/admin/super/reviews/pending` | Unverified reviews |
| PATCH | `/api/admin/super/reviews/{id}/verify` | Verify review |
| DELETE | `/api/admin/super/reviews/{id}` | Delete review |
| GET | `/api/admin/super/notifications/failed` | Delivery failures |
| GET | `/api/admin/super/wallets/customers` | All customer wallet balances |
| GET | `/api/admin/super/wallets/tenants` | All tenant revenue wallets |
