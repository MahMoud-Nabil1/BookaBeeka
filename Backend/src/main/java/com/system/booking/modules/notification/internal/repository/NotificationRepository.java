package com.system.booking.modules.notification.internal.repository;

import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.internal.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the {@link Notification} entity.
 *
 * <p><b>Strict Multi-Tenancy Guarantee:</b>
 * Every query method in this repository requires a {@code tenantId} parameter to prevent
 * cross-tenant data exposure. Because customer accounts are global across the SaaS platform,
 * notification queries must filter by both {@code tenantId} and {@code customerId} so a customer
 * only retrieves notifications belonging to the specific tenant context.</p>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Finds all notifications scoped to both tenantId and customerId, ordered newest first.
     *
     * @param tenantId   The tenant scope.
     * @param customerId The customer whose notifications are being retrieved.
     * @return List of matching notifications.
     */
    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.customer.id = :customerId ORDER BY n.createdAt DESC")
    List<Notification> findByTenantIdAndCustomerId(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId
    );

    /**
     * Paginated retrieval of notifications for a specific customer under a specific tenant.
     * Used by customer portal endpoints.
     *
     * @param tenantId   The tenant scope.
     * @param customerId The customer ID.
     * @param pageable   Pagination and sorting parameters.
     * @return A page of notifications.
     */
    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.customer.id = :customerId ORDER BY n.createdAt DESC")
    Page<Notification> findByTenantIdAndCustomerId(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            Pageable pageable
    );

    /**
     * Finds a single notification by its ID and tenantId.
     * Ensures that even if a valid notification UUID is supplied, it cannot be accessed
     * unless it matches the tenant context of the caller.
     *
     * @param tenantId The tenant scope.
     * @param id       The notification primary key UUID.
     * @return An Optional containing the notification if found within this tenant.
     */
    Optional<Notification> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Finds a single notification by its ID and customer ID.
     * Used by customer endpoints to ensure a customer cannot read notifications belonging to others.
     *
     * @param id         The notification primary key UUID.
     * @param customerId The customer UUID.
     * @return An Optional containing the notification if found for this customer.
     */
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.customer.id = :customerId")
    Optional<Notification> findByIdAndCustomerId(
            @Param("id") UUID id,
            @Param("customerId") UUID customerId
    );

    /**
     * Finds notifications filtered by tenant, customer, and delivery status.
     *
     * @param tenantId   The tenant scope.
     * @param customerId The customer ID.
     * @param status     The notification lifecycle status.
     * @param pageable   Pagination parameters.
     * @return Paginated notifications matching the criteria.
     */
    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.customer.id = :customerId AND n.status = :status ORDER BY n.createdAt DESC")
    Page<Notification> findByTenantIdAndCustomerIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("status") NotificationStatus status,
            Pageable pageable
    );

    /**
     * Paginated list of all notifications for a tenant (used by hotel staff/admin).
     *
     * @param tenantId The tenant scope.
     * @param pageable Pagination parameters.
     * @return Page of notifications for the tenant.
     */
    Page<Notification> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Counts the total number of notifications for a customer within a tenant.
     *
     * @param tenantId   The tenant scope.
     * @param customerId The customer ID.
     * @return Count of notifications.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.customer.id = :customerId")
    long countByTenantIdAndCustomerId(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId
    );

    /**
     * Finds failed notifications eligible for scheduled retry.
     * Selects records in FAILED status that have not exceeded the maximum retry attempts,
     * ordered by creation time ascending (FIFO).
     *
     * @param status     Target status (typically NotificationStatus. FAILED).
     * @param maxRetries Maximum allowed retry attempts.
     * @param pageable   Pagination/batch limit.
     * @return List of retryable notifications.
     */
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < :maxRetries ORDER BY n.createdAt ASC")
    List<Notification> findRetryableNotifications(
            @Param("status") NotificationStatus status,
            @Param("maxRetries") int maxRetries,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Notification n WHERE n.customer.id = :customerId")
    void deleteByCustomerId(@Param("customerId") UUID customerId);
}
