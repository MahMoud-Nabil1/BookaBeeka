package com.system.booking.modules.notification.internal.service;

import com.system.booking.modules.booking.internal.entity.Booking;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.notification.api.dto.NotificationResponseDto;
import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.internal.entity.Notification;
import com.system.booking.modules.notification.internal.exception.NotificationNotFoundException;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service managing the lifecycle and persistence of {@link Notification} entities.
 *
 * <p><b>Transaction Boundary Architecture:</b>
 * Because notification processing is initiated asynchronously after the caller's transaction
 * has committed (via {@code TransactionPhase.AFTER_COMMIT}), all state-mutating methods in this
 * service run with {@link Propagation#REQUIRES_NEW}. This guarantees that:</p>
 * <ol>
 *   <li>The initial {@link NotificationStatus#PENDING} record is safely written and committed to
 *       the database before the external SMTP call is attempted.</li>
 *   <li>State updates to {@link NotificationStatus#SENT} or {@link NotificationStatus#FAILED} run
 *       in clean, isolated transactions and are never compromised by external socket timeouts.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailSenderService emailSenderService;
    private final EmailTemplateService emailTemplateService;
    private final EntityManager entityManager;

    /**
     * Complete lifecycle processor for a notification event.
     * <ol>
     *   <li>Renders the email template using {@link EmailTemplateService}.</li>
     *   <li>Persists the notification record with {@link NotificationStatus#PENDING}.</li>
     *   <li>Dispatches the rendered HTML email via {@link EmailSenderService}.</li>
     *   <li>Transitions the record to {@link NotificationStatus#SENT} or {@link NotificationStatus#FAILED}.</li>
     * </ol>
     *
     * @param event The notification event payload.
     * @return The resulting Notification entity in its final state.
     */
    public Notification processNotification(NotificationEvent event) {
        log.info("Processing notification event: type=[{}], recipient=[{}], tenant=[{}]",
                event.type(), event.recipientEmail(), event.tenantId());

        // Step 1: Render HTML template
        java.util.Map<String, Object> model = new java.util.HashMap<>(event.metadata());
        model.putIfAbsent("subject", event.subject());
        model.putIfAbsent("body", event.body());
        String renderedContent = emailTemplateService.renderEmail(event.type(), model);

        // Step 2: Persist initial PENDING record in an independent transaction
        Notification notification = null;
        try {
            notification = savePendingNotification(event, renderedContent);
        } catch (Exception ex) {
            log.warn("Failed to persist pending notification for recipient [{}]: {}. Continuing with email dispatch.",
                    event.recipientEmail(), ex.getMessage());
        }

        // Step 3: Attempt email dispatch
        try {
            emailSenderService.sendEmail(event.recipientEmail(), event.subject(), renderedContent, true);
            // Step 4a: Mark as SENT if notification was persisted
            if (notification != null && notification.getId() != null) {
                return markAsSent(notification.getId());
            }
            return notification;
        } catch (Exception ex) {
            log.error("Failed to deliver notification to [{}]. Reason: {}", event.recipientEmail(), ex.getMessage());
            // Step 4b: Mark as FAILED for auditing and retry eligibility
            if (notification != null && notification.getId() != null) {
                return markAsFailed(notification.getId(), ex.getMessage());
            }
            return notification;
        }
    }

    /**
     * Persists a new notification in {@link NotificationStatus#PENDING} status.
     *
     * @param event The source event.
     * @return The persisted Notification entity.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification savePendingNotification(NotificationEvent event) {
        return savePendingNotification(event, event.body());
    }

    /**
     * Persists a new notification with explicit body content in {@link NotificationStatus#PENDING} status.
     *
     * <p>Uses JPA entity proxies ({@code getReference}) for {@link Customer} and {@link Booking}
     * to avoid redundant SQL SELECT statements before the INSERT.</p>
     *
     * @param event       The source event.
     * @param contentBody The message content body (HTML or plain text).
     * @return The persisted Notification entity.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification savePendingNotification(NotificationEvent event, String contentBody) {
        Customer customerProxy = null;
        if (event.customerId() != null) {
            try {
                customerProxy = entityManager.getReference(Customer.class, event.customerId());
            } catch (Exception ex) {
                log.debug("Could not resolve customer reference for customerId [{}]: {}", event.customerId(), ex.getMessage());
            }
        }

        Booking bookingProxy = null;
        if (event.bookingId() != null) {
            try {
                bookingProxy = entityManager.getReference(Booking.class, event.bookingId());
            } catch (Exception ex) {
                log.debug("Could not resolve booking reference for bookingId [{}]: {}", event.bookingId(), ex.getMessage());
            }
        }

        Notification notification = Notification.builder()
                .tenantId(event.tenantId())
                .customer(customerProxy)
                .booking(bookingProxy)
                .type(event.type())
                .subject(event.subject())
                .body(contentBody != null ? contentBody : event.body())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved as PENDING: id=[{}], tenant=[{}], customer=[{}]",
                saved.getId(), saved.getTenantId(), event.customerId());
        return saved;
    }

    /**
     * Transitions an existing notification to {@link NotificationStatus#SENT}.
     *
     * @param notificationId The primary key UUID of the notification.
     * @return The updated Notification entity.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification markAsSent(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notification.setFailureReason(null);
        Notification updated = notificationRepository.save(notification);

        log.info("Notification marked as SENT: id=[{}], sentAt=[{}]", updated.getId(), updated.getSentAt());
        return updated;
    }

    /**
     * Transitions an existing notification to {@link NotificationStatus#FAILED}.
     *
     * @param notificationId The primary key UUID of the notification.
     * @param failureReason  Diagnostic reason for the failure.
     * @return The updated Notification entity.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification markAsFailed(UUID notificationId, String failureReason) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailureReason(failureReason);
        int retries = (notification.getRetryCount() != null) ? notification.getRetryCount() : 0;
        notification.setRetryCount(retries + 1);
        Notification updated = notificationRepository.save(notification);

        log.warn("Notification marked as FAILED: id=[{}], retryCount=[{}], reason=[{}]",
                updated.getId(), updated.getRetryCount(), failureReason);
        return updated;
    }

    /**
     * Retries delivery for a notification in {@link NotificationStatus#FAILED} status.
     * Runs in an independent transaction.
     *
     * @param notificationId The notification UUID to retry.
     * @param maxRetries     The ceiling limit of allowed retries.
     * @return True if delivery succeeded and transitioned to SENT, false otherwise.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean retryNotification(UUID notificationId, int maxRetries) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Cannot retry missing notification id=[{}]", notificationId);
            return false;
        }

        int currentRetries = (notification.getRetryCount() != null) ? notification.getRetryCount() : 0;
        notification.setRetryCount(currentRetries + 1);

        try {
            Customer customer = notification.getCustomer();
            String recipient = (customer != null) ? customer.getEmail() : null;
            if (recipient == null || recipient.isBlank()) {
                throw new IllegalStateException("Recipient customer email is missing");
            }

            emailSenderService.sendEmail(recipient, notification.getSubject(), notification.getBody(), true);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
            notificationRepository.save(notification);

            log.info("Retry succeeded for notification id=[{}], attempt=[{}]", notificationId, currentRetries + 1);
            return true;
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
            notificationRepository.save(notification);

            log.warn("Retry attempt [{}/{}] failed for notification id=[{}]: {}",
                    currentRetries + 1, maxRetries, notificationId, ex.getMessage());
            return false;
        }
    }

    /**
     * Retrieves paginated notifications for a specific customer strictly filtered by tenantId.
     *
     * @param tenantId   The tenant ID scope.
     * @param customerId The customer ID.
     * @param pageable   Pagination parameters.
     * @return Page of notification DTOs.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsForCustomer(UUID tenantId, UUID customerId, Pageable pageable) {
        return notificationRepository.findByTenantIdAndCustomerId(tenantId, customerId, pageable)
                .map(NotificationResponseDto::fromEntity);
    }

    /**
     * Retrieves a single notification by ID strictly within a tenant's scope.
     *
     * @param tenantId       The tenant ID scope.
     * @param notificationId The notification ID.
     * @return The mapped NotificationResponseDto.
     * @throws NotificationNotFoundException if the notification does not exist under the given tenant.
     */
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationById(UUID tenantId, UUID notificationId) {
        return notificationRepository.findByTenantIdAndId(tenantId, notificationId)
                .map(NotificationResponseDto::fromEntity)
                .orElseThrow(() -> new NotificationNotFoundException(tenantId, notificationId));
    }

    /**
     * Retrieves a single notification by ID strictly for the calling customer.
     * Ensures the notification belongs to the customer regardless of tenant context.
     *
     * @param customerId     The customer ID.
     * @param notificationId The notification ID.
     * @return The mapped NotificationResponseDto.
     * @throws NotificationNotFoundException if the notification does not exist or does not belong to the customer.
     */
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationForCustomer(UUID customerId, UUID notificationId) {
        return notificationRepository.findByIdAndCustomerId(notificationId, customerId)
                .map(NotificationResponseDto::fromEntity)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }

    /**
     * Retrieves all notifications for a tenant (used by hotel staff/management).
     *
     * @param tenantId The tenant scope.
     * @param pageable Pagination parameters.
     * @return Page of notification DTOs.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsForTenant(UUID tenantId, Pageable pageable) {
        return notificationRepository.findByTenantId(tenantId, pageable)
                .map(NotificationResponseDto::fromEntity);
    }
}
