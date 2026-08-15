package com.system.booking.modules.security.context;

/**
 * Thread-local holder for the current request's {@link TenantContext}.
 *
 * <p>This is the backbone of multi-tenant data isolation. Every tenant-aware module
 * calls {@code TenantContextHolder.getContext()} to retrieve the current tenant
 * and branch scope for query filtering.</p>
 *
 * <h3>How it works:</h3>
 * <ol>
 *   <li>The {@code JwtAuthenticationFilter} extracts {@code tenant_id} and {@code branch_id}
 *       from the Staff JWT and calls {@link #setContext(TenantContext)}.</li>
 *   <li>Downstream services call {@link #getContext()} or {@link #getRequiredContext()}
 *       to scope their database queries.</li>
 *   <li>After the request completes, the filter calls {@link #clear()} to prevent
 *       ThreadLocal leaks (critical in thread-pool environments).</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b> Each thread has its own isolated copy of the context,
 * so concurrent requests never interfere with each other.</p>
 */
public class TenantContextHolder {

    /** Thread-local storage for tenant context — isolated per request thread. */
    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    /**
     * Sets the tenant context for the current request thread.
     * Called by the JwtAuthenticationFilter after validating a Staff JWT.
     */
    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    /**
     * Retrieves the tenant context for the current thread.
     *
     * @return the current TenantContext, or {@code null} if not set (e.g., Customer request)
     */
    public static TenantContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Retrieves the tenant context, throwing an exception if none is set.
     *
     * <p>Use this in tenant-aware services where a missing context indicates a
     * programming error (e.g., a service that should only be called from
     * Staff-authenticated endpoints).</p>
     *
     * @return the current TenantContext, never null
     * @throws IllegalStateException if no TenantContext is set on the current thread
     */
    public static TenantContext getRequiredContext() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "No TenantContext available. This operation requires a tenant-scoped request."
            );
        }
        return context;
    }

    /**
     * Clears the tenant context from the current thread.
     * <b>Must</b> be called after request processing to prevent ThreadLocal memory leaks.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}