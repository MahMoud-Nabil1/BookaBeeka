# Booking Platform — Availability Engine & Booking Engine Architecture
### Module Design Reference (Spring Boot / Java 21 / PostgreSQL)

This document merges the strongest, most defensible patterns from the three reference
architectures you provided into a focused spec for **only two modules**: the
**Availability Engine** and the **Booking (Reservation) Engine** — the two modules
where correctness (no double-booking) and performance matter most.

Backend stack assumed: **Java 21, Spring Boot 3.3, Spring Data JPA / Hibernate, PostgreSQL 16.**

---

## 1. Where These Modules Sit

```
+------------------+      +----------------------+      +----------------------+
|  INVENTORY       | <--> |  AVAILABILITY ENGINE  | <--> |  BOOKING ENGINE       |
|  (Resources)     |      |  (Rules/Slots/Locks)  |      |  (State Machine)      |
+------------------+      +----------------------+      +----------------------+
                                                                   |
                                                                   v
                                                     +----------------------+
                                                     |  PAYMENT / NOTIFICATION |
                                                     |  (via domain events)   |
                                                     +----------------------+
```

- Both modules are **package-private internally**, exposing only a `*ModuleApi` interface + DTOs.
- No module injects another module's `JpaRepository` or entity directly (enforced via ArchUnit, see §7).
- Every table carries `tenant_id`; Postgres **Row-Level Security (RLS)** is the safety net behind
  application-level tenant filtering (`TenantContextHolder`).

```
com.system.booking.modules.availability
 ├── api/            (AvailabilityModuleApi, DTOs — public contract)
 ├── internal/
 │    ├── entity/     (ScheduleRule, AvailabilityException, AvailabilitySlot, SlotLock)
 │    ├── repository/
 │    └── service/
 └── config/

com.system.booking.modules.booking
 ├── api/            (BookingModuleApi, DTOs — public contract)
 ├── internal/
 │    ├── entity/     (Booking, BookingStateTransition, CancellationPolicy)
 │    ├── repository/
 │    └── service/
 └── config/
```

---

## 2. Availability Engine

### 2.1 Responsibilities

- Store **recurring schedule rules** per resource (weekly working hours).
- Store **one-off exceptions** (blackout days, holidays, special hours).
- **Generate bookable slots** on demand from rules − exceptions − existing bookings (no
  pre-materialized slot table required for MVP scale; a materialized `availability_slots`
  table is used only if the resource type needs hard capacity counters, e.g. multi-seat
  classes or hotel rooms).
- Apply **buffer time** (cleanup/travel gaps) around bookings.
- Provide **temporary soft-locks** on a slot while a customer completes checkout.
- Guarantee **zero double-booking** in cooperation with the Booking Engine's DB constraints.
- Publish availability-changed signals so cached calendars can be invalidated.

### 2.2 Domain Entities

| Entity | Purpose |
|---|---|
| `ScheduleRule` | Recurring weekly template: `resourceId`, `dayOfWeek`, `startTime`, `endTime`. |
| `AvailabilityException` | Date-specific override: blackout (`isAvailable=false`) or special hours. |
| `AvailabilitySlot` *(optional, capacity-based resources only)* | Pre-materialized slot with `maxCapacity`, `currentOccupancy`, `version` (optimistic lock). |
| `SlotLock` | Temporary hold: `slotId`/`resourceId`+`timeRange`, `userId`, `expiresAt`, `status`. |

### 2.3 Table DDL (core)

```sql
CREATE TABLE schedule_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT valid_time_range CHECK (start_time < end_time)
);

CREATE TABLE availability_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    exception_date DATE NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT false,
    start_time TIME,
    end_time TIME,
    reason TEXT,
    UNIQUE (resource_id, exception_date)
);

CREATE TABLE slot_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    slot_start TIMESTAMPTZ NOT NULL,
    slot_end TIMESTAMPTZ NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','EXPIRED','CONSUMED','RELEASED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_slot_locks_resource_time ON slot_locks(resource_id, slot_start, slot_end)
    WHERE status = 'ACTIVE';
```

### 2.4 Slot Generation Query (range subtraction, DB-driven)

Generates candidate slots from `schedule_rules`, removes anything covered by
`availability_exceptions` (blackouts) and overlapping active `bookings`:

```sql
WITH candidate_slots AS (
    SELECT generate_series(
        (:date::date + start_time)::timestamptz,
        (:date::date + end_time)::timestamptz - (:duration || ' minutes')::interval,
        (:duration || ' minutes')::interval
    ) AS slot_start
    FROM schedule_rules
    WHERE resource_id = :resourceId
      AND day_of_week = EXTRACT(DOW FROM :date::date)
),
blocked AS (
    SELECT tstzrange(start_time, end_time + (:bufferMinutes || ' minutes')::interval) AS range
    FROM bookings
    WHERE resource_id = :resourceId AND status != 'CANCELLED'
    UNION ALL
    SELECT tstzrange(slot_start, slot_end)
    FROM slot_locks
    WHERE resource_id = :resourceId AND status = 'ACTIVE' AND expires_at > now()
)
SELECT slot_start, slot_start + (:duration || ' minutes')::interval AS slot_end
FROM candidate_slots
WHERE NOT EXISTS (
    SELECT 1 FROM blocked WHERE range && tstzrange(slot_start, slot_start + (:duration || ' minutes')::interval)
)
AND NOT EXISTS (
    SELECT 1 FROM availability_exceptions
    WHERE resource_id = :resourceId AND exception_date = :date AND is_available = false
);
```

**Notes**
- All timestamps are `TIMESTAMPTZ` (UTC) — DST-safe. Wall-clock hours are anchored to the
  tenant's timezone only for display/generation input, never for storage.
- Buffer time is subtracted by widening the blocked range, not by shrinking the slot grid.
- Result is cached (`slots:{tenantId}:{resourceId}:{date}`, Redis, TTL ~5 min) and invalidated
  on booking create/cancel/reschedule or rule/exception change.

### 2.5 `AvailabilityModuleApi` (public contract)

```java
public interface AvailabilityModuleApi {

    List<SlotDto> getAvailableSlots(UUID tenantId, UUID resourceId, LocalDate date);

    SlotLockDto lockSlot(UUID tenantId, UUID resourceId, Instant start, Instant end, UUID userId);

    void releaseLock(UUID tenantId, UUID lockId);

    void consumeLock(UUID tenantId, UUID lockId, UUID bookingId);

    boolean isRangeAvailable(UUID tenantId, UUID resourceId, Instant start, Instant end);

    void defineScheduleRule(UUID tenantId, UUID resourceId, ScheduleRuleDto rule);

    void addAvailabilityException(UUID tenantId, UUID resourceId, ExceptionDto exception);
}
```

### 2.6 Methods to Implement — Internal Services

**`ScheduleRuleService`**
- `createRule(...)`, `updateRule(...)`, `deleteRule(...)`
- `listRulesForResource(resourceId)`
- `validateNoOverlappingRules(resourceId, dayOfWeek, start, end)`

**`AvailabilityExceptionService`**
- `addBlackoutDate(resourceId, date, reason)`
- `addSpecialHours(resourceId, date, start, end)`
- `removeException(exceptionId)`

**`SlotGenerationService`**
- `generateSlots(tenantId, resourceId, date, durationMinutes, bufferMinutes)` → runs §2.4 query
- `generateSlotsForRange(resourceId, startDate, endDate)` — batched, for calendar views
- `invalidateCache(resourceId, date)`

**`SlotLockingService`** (concurrency-critical)
- `acquireTemporaryLock(tenantId, resourceId, start, end, userId)`
  - Uses `SELECT ... FOR UPDATE` (or a `tstzrange` exclusion constraint on `slot_locks` +
    `bookings` combined) to serialize concurrent hold attempts on the same window.
  - Sets `expiresAt = now() + 10 minutes`.
  - Throws `SlotUnavailableException` → mapped to HTTP 409 if the range is already
    locked or booked.
- `releaseLock(lockId)` — manual release (checkout abandoned).
- `expireStaleLocks()` — scheduled job (`@Scheduled` every 60s) sweeping `ACTIVE` locks past
  `expires_at` to `EXPIRED`, freeing the range.
- `consumeLock(lockId, bookingId)` — transitions `ACTIVE → CONSUMED` inside the same
  transaction that creates the confirmed booking.

### 2.7 Concurrency Guarantee (shared with Booking Engine)

| Layer | Mechanism |
|---|---|
| 1. Exclusion constraint (final safety net) | GiST index on `bookings(resource_id, tstzrange(start_time,end_time))` rejecting overlap at the DB level, independent of application bugs |
| 2. Row locking | `SELECT ... FOR UPDATE` on the conflict-check query inside `acquireTemporaryLock` / `createBooking` |
| 3. Soft-lock TTL | 10-minute `slot_locks` hold, swept by a scheduled expiry job |
| 4. Optimistic version | `version` column on `bookings` / capacity-based `availability_slots` to prevent lost updates |

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING gist (
    resource_id WITH =,
    tstzrange(start_time, end_time) WITH &&
) WHERE (status != 'CANCELLED');
```

---

## 3. Booking Engine

### 3.1 Responsibilities

- Orchestrate the full reservation flow: **validate → hold → confirm → cancel/reschedule**.
- Enforce a strict **finite state machine** on booking status.
- Guarantee **idempotency** on booking creation (retries/double-submits never double-book).
- Apply **cancellation/refund policy** rules based on time-to-slot.
- Publish domain events for Payment and Notification modules (never call them directly).
- Never touch `resources` or `slot_locks` tables directly — only via `InventoryModuleApi`
  and `AvailabilityModuleApi`.

### 3.2 Domain Entities

| Entity | Purpose |
|---|---|
| `Booking` | Core transactional record: tenant, customer, resource, time range, status, `version`, `metadata` (JSONB). |
| `BookingStateTransition` | Audit trail of every status change (who, when, from→to, reason). |
| `CancellationPolicy` | Tenant-configured refund tiers relative to time-before-slot. |
| `IdempotencyKey` | `(tenant_id, key)` composite PK guarding exactly-once creation. |

### 3.3 Table DDL (core)

```sql
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    resource_id UUID NOT NULL REFERENCES resources(id),
    lock_id UUID REFERENCES slot_locks(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (status IN ('PENDING_PAYMENT','CONFIRMED','CANCELLED','COMPLETED','EXPIRED')),
    total_amount NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    cancellation_reason TEXT,
    metadata JSONB NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_bookings_tenant_customer ON bookings(tenant_id, customer_id);
CREATE INDEX idx_bookings_resource_time ON bookings(resource_id, start_time, end_time);
CREATE INDEX idx_bookings_status ON bookings(status);

CREATE TABLE booking_state_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason TEXT,
    actor_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE idempotency_keys (
    tenant_id UUID NOT NULL,
    key TEXT NOT NULL,
    response_status INTEGER,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, key)
);
```

### 3.4 Booking State Machine

```
PENDING_PAYMENT --(payment success)--> CONFIRMED --(slot time passes)--> COMPLETED
PENDING_PAYMENT --(timeout / payment fail)--> EXPIRED
CONFIRMED       --(customer/admin cancels, policy checked)--> CANCELLED
```

Illegal transitions (e.g. `CANCELLED → CONFIRMED`, `COMPLETED → CANCELLED`) are rejected by
`BookingStateMachine.assertTransitionAllowed(from, to)` and never reach the repository.

### 3.5 `BookingModuleApi` (public contract)

```java
public interface BookingModuleApi {

    BookingConfirmationDto createBooking(CreateBookingRequestDto request, String idempotencyKey);

    void confirmBooking(UUID tenantId, UUID bookingId);

    CancellationResultDto cancelBooking(UUID tenantId, UUID bookingId, String reason, UUID actorId);

    BookingDto rescheduleBooking(UUID tenantId, UUID bookingId, Instant newStart, Instant newEnd);

    BookingStatusDto getBookingStatus(UUID tenantId, UUID bookingId);

    List<BookingDto> listBookingsForCustomer(UUID tenantId, UUID customerId);
}
```

### 3.6 Methods to Implement — Internal Services

**`BookingCreationService.createBooking(request, idempotencyKey)`** — the critical path:
1. `idempotencyService.begin(tenantId, idempotencyKey)` — atomic insert with
   `ON CONFLICT DO NOTHING`; if a row already existed, return the stored response instead
   of re-running business logic.
2. `inventoryApi.isResourceBookable(tenantId, resourceId)` — via contract, not repository.
3. `availabilityApi.lockSlot(tenantId, resourceId, start, end, customerId)` — acquires the
   10-minute soft-lock (§2.6); throws → HTTP 409 on conflict.
4. Insert `Booking` row with status `PENDING_PAYMENT` (exclusion constraint is the final
   guard even if the soft-lock step is somehow bypassed).
5. `availabilityApi.consumeLock(lockId, booking.getId())`.
6. `idempotencyService.complete(tenantId, idempotencyKey, response)`.
7. Publish `BookingCreatedEvent` (AFTER_COMMIT).

**`BookingLifecycleService`**
- `confirmBooking(bookingId)` — called on payment-success webhook; `PENDING_PAYMENT → CONFIRMED`
  with optimistic version check; publishes `BookingConfirmedEvent` (`AFTER_COMMIT`).
- `expireBooking(bookingId)` — scheduled sweep for `PENDING_PAYMENT` bookings whose lock
  expired without payment; `PENDING_PAYMENT → EXPIRED`; releases the lock.
- `cancelBooking(bookingId, reason, actorId)`:
  - `cancellationPolicyService.calculateRefund(booking, now())`
  - `CONFIRMED → CANCELLED` with version check; records `BookingStateTransition`.
  - Publishes `BookingCancelledEvent` (triggers refund + notification).
- `rescheduleBooking(bookingId, newStart, newEnd)` — implemented as **cancel + create-new
  inside one transaction**, guarded by the exclusion constraint and version checks so a
  concurrent cancellation/reschedule race can never leave an inconsistent state.
- `completeBooking(bookingId)` — scheduled sweep: `CONFIRMED → COMPLETED` once `end_time` passes.

**`CancellationPolicyService`**
- `calculateRefund(booking, requestedAt)` — applies tenant-configured tiers (e.g. full refund
  ≥48h before slot, 50% within 24h, 0% inside cutoff).
- `getPolicyForTenant(tenantId)`

**`IdempotencyService`**
- `begin(tenantId, key)` → `Optional<StoredResponse>` (empty if new)
- `complete(tenantId, key, status, body)`

**`BookingStateMachine`**
- `assertTransitionAllowed(BookingStatus from, BookingStatus to)`
- `nextValidStates(BookingStatus current)`

### 3.7 Optimistic Locking on Updates

```sql
UPDATE bookings
SET status = :newStatus, version = version + 1, updated_at = now()
WHERE id = :bookingId AND version = :expectedVersion AND tenant_id = :tenantId;
-- 0 rows affected => stale read, retry from a fresh SELECT
```
Or the JPA-native equivalent via `@Version` on the `Booking` entity, letting Hibernate throw
`OptimisticLockException` → mapped to `409 Conflict` by a `@RestControllerAdvice`.

### 3.8 Domain Events Published

| Event | Payload | Consumers |
|---|---|---|
| `BookingCreatedEvent` | bookingId, tenantId, customerId, resourceId, amount, timestamp | Payment module |
| `BookingConfirmedEvent` | bookingId, tenantId, customerId, resourceId, slotRange | Notification module (confirmation email + `.ics`) |
| `BookingCancelledEvent` | bookingId, refundAmount, reason | Payment module (refund), Notification module |
| `BookingExpiredEvent` | bookingId | Notification module (optional), Availability (lock cleanup confirmation) |

All listeners use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` +
`@Async` so a rollback never triggers a false-positive email or charge.

---

## 4. Cross-Module Sequence — Happy Path

```
Customer          Booking Engine            Availability Engine        Payment
   |  POST /bookings   |                            |                     |
   |------------------>|                            |                     |
   |                    |-- idempotency begin ------>|                     |
   |                    |-- isResourceBookable ------>(Inventory)          |
   |                    |-- lockSlot(start,end) ----->|                     |
   |                    |                            |-- FOR UPDATE check   |
   |                    |<-- SlotLockDto (10m TTL) ---|                     |
   |                    |-- INSERT booking (PENDING) |                     |
   |                    |-- consumeLock -------------->|                     |
   |<-- 201 booking id -|                            |                     |
   |  redirect to pay   |                            |                     |
   |------------------------------------------------------------------->  |
   |                    |<--------------------- webhook: payment SUCCESS --|
   |                    |-- confirmBooking (CONFIRMED)|                     |
   |                    |-- publish BookingConfirmedEvent (AFTER_COMMIT)   |
```

---

## 5. Idempotency, Concurrency & Testing — Mandatory Test Matrix

| Test | Expected Result |
|---|---|
| 1,000 concurrent bookings on the same slot | Exactly 1 succeeds; 999 return `409 Conflict` |
| Duplicate request, same `Idempotency-Key` | Second call returns the first response, no new row |
| Concurrent cancel + reschedule on same booking | Final state consistent; loser gets `409` via version check |
| Lock expiry without payment | Booking auto-transitions to `EXPIRED`; slot freed within one sweep interval |
| Overlapping insert bypassing app logic (simulated) | Exclusion constraint rejects at DB level |
| Cross-tenant booking read | RLS returns empty set even with a crafted query |

Run all concurrency tests against a **real PostgreSQL** instance (Testcontainers), never
an in-memory substitute — exclusion constraints and `FOR UPDATE` semantics don't exist in H2.

---

## 6. Scheduled Jobs Summary

| Job | Frequency | Action |
|---|---|---|
| `SlotLockExpirySweep` | every 60s | `ACTIVE → EXPIRED` on locks past `expires_at` |
| `PendingBookingExpirySweep` | every 60s | `PENDING_PAYMENT → EXPIRED` once the linked lock expired |
| `BookingCompletionSweep` | hourly | `CONFIRMED → COMPLETED` once `end_time` has passed |
| `AvailabilityCacheWarmup` (optional) | on schedule/exception change | Pre-computes next 30 days of slots for active resources |

---

## 7. Boundary Enforcement (ArchUnit)

```java
@Test
void bookingModuleMustNotAccessAvailabilityInternals() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.system.booking.modules");
    ArchRuleDefinition.noClasses()
        .that().resideInAPackage("..modules.booking.internal..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..modules.availability.internal..",
            "..modules.inventory.internal..")
        .because("Cross-module access must go through the public *ModuleApi contract only")
        .check(classes);
}
```

---

## 8. Summary — Method Checklist

**Availability Engine**
- [ ] `ScheduleRuleService`: create/update/delete/list rules, overlap validation
- [ ] `AvailabilityExceptionService`: add blackout, add special hours, remove
- [ ] `SlotGenerationService`: generateSlots, generateSlotsForRange, invalidateCache
- [ ] `SlotLockingService`: acquireTemporaryLock, releaseLock, expireStaleLocks, consumeLock
- [ ] `AvailabilityModuleApi`: getAvailableSlots, lockSlot, releaseLock, consumeLock, isRangeAvailable

**Booking Engine**
- [ ] `BookingCreationService`: createBooking (idempotent, locking, event publish)
- [ ] `BookingLifecycleService`: confirmBooking, expireBooking, cancelBooking, rescheduleBooking, completeBooking
- [ ] `CancellationPolicyService`: calculateRefund, getPolicyForTenant
- [ ] `IdempotencyService`: begin, complete
- [ ] `BookingStateMachine`: assertTransitionAllowed, nextValidStates
- [ ] `BookingModuleApi`: createBooking, confirmBooking, cancelBooking, rescheduleBooking, getBookingStatus, listBookingsForCustomer
