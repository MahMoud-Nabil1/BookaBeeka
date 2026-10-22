package com.system.booking.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Application-level caching configuration using Caffeine (in-process, single-node).
 *
 * <p><b>Cache strategy:</b>
 * <ul>
 *   <li>Reference / lookup data (room types, amenities, services, tenants):
 *       30-minute TTL — changes are infrequent and evictions are triggered explicitly
 *       on every write via {@code @CacheEvict}.</li>
 *   <li>Aggregate / dashboard data (owner dashboard, revenue summary):
 *       5-minute TTL — computed from multiple DB queries; slightly stale is acceptable.</li>
 * </ul>
 *
 * <p><b>Neon cold-start note:</b>
 * Cache hits skip the database entirely, which is the most effective mitigation
 * against Neon compute scale-to-zero latency. Keep TTLs reasonable so reads don't
 * serve stale data indefinitely.
 *
 * <p><b>Upgrade path:</b>
 * If the application is scaled to multiple instances, swap this for a Redis-backed
 * {@code CacheManager} (add {@code spring-boot-starter-data-redis} and configure
 * {@code RedisCacheManager}). Cache name constants remain the same.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // ── Cache name constants ───────────────────────────────────────────────────
    // Use these constants in @Cacheable / @CacheEvict / @CachePut to avoid
    // magic strings scattered across the codebase.

    /** Tenant lookup by ID — hit on every authenticated cross-module call. */
    public static final String CACHE_TENANTS            = "tenants";

    /** Tenant lookup by subdomain — called on public hotel search. */
    public static final String CACHE_TENANT_SUBDOMAIN   = "tenantBySubdomain";

    /** All room types for a tenant — reference data, rarely changes. */
    public static final String CACHE_ROOM_TYPES         = "roomTypes";

    /** All rooms (resources) for a tenant — reference data. */
    public static final String CACHE_RESOURCES          = "resources";

    /** Single room lookup by tenantId + resourceId. */
    public static final String CACHE_RESOURCE           = "resource";

    /** All amenities for a tenant — very stable reference data. */
    public static final String CACHE_AMENITIES          = "amenities";

    /** All service offerings for a tenant — reference data. */
    public static final String CACHE_SERVICE_OFFERINGS  = "serviceOfferings";

    /** Owner dashboard — expensive 4-query aggregate, 5-min TTL. */
    public static final String CACHE_OWNER_DASHBOARD    = "ownerDashboard";

    /** Owner revenue summary — expensive aggregate, 5-min TTL. */
    public static final String CACHE_OWNER_REVENUE      = "ownerRevenue";

    // ── CacheManager ──────────────────────────────────────────────────────────

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Register each cache with its own Caffeine spec (TTL + max size).
        // recordStats() enables Caffeine hit/miss metrics.

        // Reference data: 30-minute TTL, max 500 entries per cache
        manager.registerCustomCache(CACHE_TENANTS,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_TENANT_SUBDOMAIN,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_ROOM_TYPES,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_RESOURCES,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(2000)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_RESOURCE,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(2000)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_AMENITIES,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_SERVICE_OFFERINGS,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());

        // Aggregates: 5-minute TTL (acceptable staleness for dashboards)
        manager.registerCustomCache(CACHE_OWNER_DASHBOARD,
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_OWNER_REVENUE,
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build());

        return manager;
    }
}
