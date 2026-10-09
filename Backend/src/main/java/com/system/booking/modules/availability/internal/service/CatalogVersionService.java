package com.system.booking.modules.availability.internal.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory version tracker for the public hotel & room catalog.
 *
 * <p>Incremented whenever a hotel is suspended, unsuspended, deleted, or inventory changes.
 * Public clients poll this version periodically (e.g. 20s) and refresh their queries
 * only when the returned version exceeds their cached version.</p>
 *
 * <p><b>Limitation:</b> This in-memory counter is scoped to a single backend process.
 * In a multi-node horizontal deployment, a shared Redis key or SSE/WebSocket topic
 * would replace the in-memory AtomicLong.</p>
 */
@Service
public class CatalogVersionService {

    private final AtomicLong catalogVersion = new AtomicLong(1L);

    public long getVersion() {
        return catalogVersion.get();
    }

    public long increment() {
        return catalogVersion.incrementAndGet();
    }
}
