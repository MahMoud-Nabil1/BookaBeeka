package com.system.booking.modules.booking.internal.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// composite key for IdempotencyKey (tenant_id + key)
public class IdempotencyKeyId implements Serializable {

    private UUID tenantId;
    private String key;

    public IdempotencyKeyId() {}

    public IdempotencyKeyId(UUID tenantId, String key) {
        this.tenantId = tenantId;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotencyKeyId that)) return false;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, key);
    }
}
