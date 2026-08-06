package com.system.booking.modules.booking.internal.service;

import com.system.booking.modules.booking.internal.entity.IdempotencyKey;
import com.system.booking.modules.booking.internal.entity.IdempotencyKeyId;
import com.system.booking.modules.booking.internal.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// prevents double-submits from creating duplicate bookings
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repo;

    // check if this key was already used — if yes, return the stored response
    public Optional<Map<String, Object>> begin(UUID tenantId, String key) {
        IdempotencyKeyId id = new IdempotencyKeyId(tenantId, key);
        Optional<IdempotencyKey> existing = repo.findById(id);

        if (existing.isPresent() && existing.get().getResponseBody() != null) {
            return Optional.of(existing.get().getResponseBody());
        }

        if (existing.isEmpty()) {
            // first time seeing this key, claim it
            IdempotencyKey record = new IdempotencyKey();
            record.setTenantId(tenantId);
            record.setKey(key);
            record.setCreatedAt(OffsetDateTime.now());
            repo.save(record);
        }

        return Optional.empty();
    }

    // save the response so replays get the same answer
    public void complete(UUID tenantId, String key, int status, Map<String, Object> body) {
        IdempotencyKeyId id = new IdempotencyKeyId(tenantId, key);
        IdempotencyKey record = repo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Idempotency key not found: " + key));
        record.setResponseStatus(status);
        record.setResponseBody(body);
        repo.save(record);
    }
}
