package com.system.booking.modules.notification;

import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.internal.entity.Notification;
import com.system.booking.modules.notification.internal.repository.NotificationRepository;
import com.system.booking.modules.notification.internal.scheduler.NotificationRetryScheduler;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.security.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRetrySchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationRetryScheduler retryScheduler;

    private UUID tenantId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        TenantContextHolder.clear();
        ReflectionTestUtils.setField(retryScheduler, "maxRetries", 3);
        ReflectionTestUtils.setField(retryScheduler, "batchSize", 50);
        ReflectionTestUtils.setField(retryScheduler, "retryEnabled", true);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("retryFailedNotifications should sweep retryable records, establish TenantContext, and retry")
    void testRetryFailedNotificationsSweep() {
        Notification notification = Notification.builder()
                .id(notificationId)
                .tenantId(tenantId)
                .status(NotificationStatus.FAILED)
                .retryCount(1)
                .build();

        when(notificationRepository.findRetryableNotifications(eq(NotificationStatus.FAILED), eq(3), any()))
                .thenReturn(List.of(notification));

        doAnswer(invocation -> {
            assertThat(TenantContextHolder.getContext()).isNotNull();
            assertThat(TenantContextHolder.getContext().tenantId()).isEqualTo(tenantId);
            return true;
        }).when(notificationService).retryNotification(notificationId, 3);

        retryScheduler.retryFailedNotifications();

        verify(notificationService).retryNotification(notificationId, 3);
        assertThat(TenantContextHolder.getContext()).isNull();
    }

    @Test
    @DisplayName("retryFailedNotifications should do nothing if disabled")
    void testRetryDisabled() {
        ReflectionTestUtils.setField(retryScheduler, "retryEnabled", false);

        retryScheduler.retryFailedNotifications();

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("retryFailedNotifications should return gracefully if no retryable notifications found")
    void testNoRetryableNotifications() {
        when(notificationRepository.findRetryableNotifications(eq(NotificationStatus.FAILED), eq(3), any()))
                .thenReturn(Collections.emptyList());

        retryScheduler.retryFailedNotifications();

        verify(notificationRepository).findRetryableNotifications(eq(NotificationStatus.FAILED), eq(3), any());
        verifyNoInteractions(notificationService);
    }
}
