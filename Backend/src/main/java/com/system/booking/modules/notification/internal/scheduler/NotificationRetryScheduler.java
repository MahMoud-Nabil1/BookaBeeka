package com.system.booking.modules.notification.internal.scheduler;

import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.internal.entity.Notification;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled background worker responsible for automatically retrying failed notifications.
 *
 * <h2>Resilience & Multi-Tenancy Architecture</h2>
 * <ol>
 *   <li><b>Fault Tolerance:</b> External SMTP servers may experience transient network timeouts,
 *       rate limits, or connection drops. This scheduler periodically sweeps for records in
 *       {@link NotificationStatus#FAILED} and attempts redelivery.</li>
 *   <li><b>Dead-Letter Protection:</b> Only notifications with {@code retryCount < maxRetries} are
 *       retried, preventing endless retry loops on permanent failures (e.g., malformed email addresses).</li>
 *   <li><b>Tenant Context Isolation:</b> Even though this job runs on a background scheduler thread,
 *       each notification's {@code tenantId} is bound to {@link TenantContextHolder} during its redelivery
 *       attempt and guaranteed to be cleaned up in a {@code finally} block.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${notification.retry.batch-size:50}")
    private int batchSize;

    @Value("${notification.retry.enabled:true}")
    private boolean retryEnabled;

    /**
     * Executes the retry sweep at fixed intervals.
     * Default: every 60 seconds, with an initial startup delay of 15 seconds.
     */
    @Scheduled(fixedDelayString = "${notification.retry.delay-ms:60000}", initialDelay = 15000)
    public void retryFailedNotifications() {
        if (!retryEnabled) {
            return;
        }

        List<Notification> retryableList = notificationRepository.findRetryableNotifications(
                NotificationStatus.FAILED,
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        if (retryableList.isEmpty()) {
            return;
        }

        log.info("[RETRY_SCHEDULER] Found [{}] failed notifications eligible for retry (maxAttempts={})",
                retryableList.size(), maxRetries);

        int succeeded = 0;
        int failed = 0;

        for (Notification notification : retryableList) {
            TenantContextHolder.setContext(new TenantContext(notification.getTenantId()));
            try {
                boolean success = notificationService.retryNotification(notification.getId(), maxRetries);
                if (success) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                log.error("[RETRY_SCHEDULER] Unexpected error retrying notification id=[{}]: {}",
                        notification.getId(), ex.getMessage());
                failed++;
            } finally {
                TenantContextHolder.clear();
            }
        }

        log.info("[RETRY_SCHEDULER] Completed retry batch: [{}] succeeded, [{}] still failed",
                succeeded, failed);
    }
}
