package com.system.booking.modules.notification;

import com.system.booking.modules.notification.api.NotificationController;
import com.system.booking.modules.notification.api.dto.NotificationResponseDto;
import com.system.booking.modules.notification.api.model.NotificationStatus;
import com.system.booking.modules.notification.api.model.NotificationType;
import com.system.booking.modules.notification.internal.exception.NotificationNotFoundException;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    private UUID tenantId;
    private UUID customerId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCustomer() {
        CustomerPrincipal principal = new CustomerPrincipal(customerId, "customer@example.com");
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsStaff() {
        HotelUserPrincipal principal = new HotelUserPrincipal(
                UUID.randomUUID(), "staff@hotel.com", "ADMIN", tenantId
        );
        TenantContextHolder.setContext(new TenantContext(tenantId));
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("getMyNotifications should return paginated notifications for logged in customer")
    void testGetMyNotifications() {
        authenticateAsCustomer();

        NotificationResponseDto dto = new NotificationResponseDto(
                notificationId, tenantId, customerId, null,
                NotificationType.BOOKING_CONFIRMED, "Confirmed", "Body",
                NotificationStatus.SENT, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<NotificationResponseDto> page = new PageImpl<>(List.of(dto));

        when(notificationService.getNotificationsForCustomer(eq(tenantId), eq(customerId), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<NotificationResponseDto>> response = controller.getMyNotifications(
                tenantId, PageRequest.of(0, 20)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).id()).isEqualTo(notificationId);
    }

    @Test
    @DisplayName("getCustomerNotificationsForStaff should use tenantId from TenantContextHolder")
    void testGetCustomerNotificationsForStaff() {
        authenticateAsStaff();

        NotificationResponseDto dto = new NotificationResponseDto(
                notificationId, tenantId, customerId, null,
                NotificationType.OTP_REQUESTED, "OTP", "Body",
                NotificationStatus.SENT, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<NotificationResponseDto> page = new PageImpl<>(List.of(dto));

        when(notificationService.getNotificationsForCustomer(eq(tenantId), eq(customerId), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<NotificationResponseDto>> response = controller.getCustomerNotificationsForStaff(
                customerId, PageRequest.of(0, 20)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getNotificationById for customer should retrieve notification via customer ID")
    void testGetNotificationByIdCustomer() {
        authenticateAsCustomer();

        NotificationResponseDto dto = new NotificationResponseDto(
                notificationId, tenantId, customerId, null,
                NotificationType.BOOKING_CONFIRMED, "Confirmed", "Body",
                NotificationStatus.SENT, LocalDateTime.now(), LocalDateTime.now()
        );

        when(notificationService.getNotificationForCustomer(customerId, notificationId))
                .thenReturn(dto);

        ResponseEntity<NotificationResponseDto> response = controller.getNotificationById(notificationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(notificationId);
    }

    @Test
    @DisplayName("handleNotificationNotFound should return 404 with error details")
    void testHandleNotFound() {
        NotificationNotFoundException ex = new NotificationNotFoundException(notificationId);
        ResponseEntity<Map<String, String>> response = controller.handleNotificationNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Notification Not Found");
    }
}
