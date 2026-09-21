package com.system.booking.modules.security;

import com.system.booking.modules.notification.api.event.NotificationEvent;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.dto.request.OtpRequest;
import com.system.booking.modules.security.dto.request.PasswordResetRequest;
import com.system.booking.modules.security.port.in.CustomerAuthPort;
import com.system.booking.modules.security.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationNotificationTest {

    @Mock
    private CustomerAuthPort customerPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("requestOtp should generate OTP and publish NotificationEvent")
    void testRequestOtpPublishesEvent() {
        UUID customerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "guest@example.com";

        AuthUserDTO user = new AuthUserDTO(customerId, email, "hash", "CUSTOMER", tenantId, true);
        when(customerPort.findCustomerByEmail(email)).thenReturn(Optional.of(user));

        authenticationService.requestOtp(new OtpRequest(email, tenantId));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.recipientEmail()).isEqualTo(email);
        assertThat(event.type()).isEqualTo(NotificationType.OTP_REQUESTED);
        assertThat(event.metadata()).containsKey("otpCode");
        assertThat((String) event.metadata().get("otpCode")).hasSize(6);
    }

    @Test
    @DisplayName("requestPasswordReset should generate token and publish NotificationEvent")
    void testRequestPasswordResetPublishesEvent() {
        UUID customerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "guest@example.com";

        AuthUserDTO user = new AuthUserDTO(customerId, email, "hash", "CUSTOMER", tenantId, true);
        when(customerPort.findCustomerByEmail(email)).thenReturn(Optional.of(user));

        authenticationService.requestPasswordReset(new PasswordResetRequest(email, tenantId));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.recipientEmail()).isEqualTo(email);
        assertThat(event.type()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(event.metadata()).containsKey("resetToken");
        assertThat(event.metadata()).containsKey("resetUrl");
    }
}
