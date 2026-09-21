package com.system.booking.modules.notification.api;

import com.system.booking.modules.notification.api.dto.NotificationResponseDto;
import com.system.booking.modules.notification.internal.exception.NotificationNotFoundException;
import com.system.booking.modules.notification.internal.service.NotificationService;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller exposing endpoints to query notifications with strict multi-tenant data isolation.
 *
 * <h2>Multi-Tenancy Isolation Architecture</h2>
 * <p>
 * This controller serves two distinct caller personas while preventing cross-tenant and cross-user leaks:
 * </p>
 * <ol>
 *   <li><b>Customers (Role: CUSTOMER):</b>
 *       Customer accounts are global across all hotels on the platform. When a customer queries
 *       {@code /my-notifications}, the {@code customerId} is extracted directly from the verified JWT
 *       via {@link SecurityUtil#getCurrentCustomerPrincipal()}. The customer provides the target
 *       {@code tenantId} query parameter, ensuring they only retrieve notifications issued by that
 *       specific hotel tenant.
 *   </li>
 *   <li><b>Hotel Staff & Owners (Roles: OWNER, ADMIN):</b>
 *       Staff members are strictly bound to a single hotel tenant. When staff queries a customer's
 *       notifications via {@code /customer/{customerId}}, the {@code tenantId} is extracted implicitly
 *       from the signed JWT claim via {@link TenantContextHolder}. Staff cannot supply or manipulate
 *       the tenantId, preventing cross-tenant inspection.
 *   </li>
 * </ol>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves notifications for the currently logged-in customer under a specific hotel tenant.
     *
     * @param tenantId The hotel tenant to filter notifications for.
     * @param pageable Pagination parameters (defaults to 20 items sorted by createdAt DESC).
     * @return Paginated list of the customer's notifications for the tenant.
     */
    @GetMapping("/my-notifications")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(
            @RequestParam UUID tenantId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();
        log.info("Customer [{}] fetching notifications for tenant [{}]", customer.id(), tenantId);

        Page<NotificationResponseDto> response = notificationService.getNotificationsForCustomer(
                tenantId,
                customer.id(),
                pageable
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves notifications for a specific customer under the calling staff member's hotel tenant.
     * Accessible only by hotel owners and administrators.
     *
     * @param customerId The customer whose notification log is being inspected.
     * @param pageable   Pagination parameters.
     * @return Paginated notifications for the customer within the staff's tenant.
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Page<NotificationResponseDto>> getCustomerNotificationsForStaff(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // tenantId is extracted exclusively from the Staff JWT via TenantContextHolder
        UUID tenantId = TenantContextHolder.getRequiredContext().tenantId();
        log.info("Staff from tenant [{}] querying notifications for customer [{}]", tenantId, customerId);

        Page<NotificationResponseDto> response = notificationService.getNotificationsForCustomer(
                tenantId,
                customerId,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single notification by ID with strict tenant boundary enforcement.
     *
     * @param id The notification ID.
     * @return The notification DTO.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponseDto> getNotificationById(@PathVariable UUID id) {
        String userType = SecurityUtil.getCurrentUserType();
        NotificationResponseDto notification;

        if ("CUSTOMER".equals(userType)) {
            CustomerPrincipal customer = SecurityUtil.getCurrentCustomerPrincipal();
            notification = notificationService.getNotificationForCustomer(customer.id(), id);
        } else {
            // Hotel staff: tenantId is derived from TenantContextHolder
            HotelUserPrincipal staff = SecurityUtil.getCurrentHotelUserPrincipal();
            notification = notificationService.getNotificationById(staff.tenantId(), id);
        }

        return ResponseEntity.ok(notification);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotificationNotFound(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Notification Not Found",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }
}
