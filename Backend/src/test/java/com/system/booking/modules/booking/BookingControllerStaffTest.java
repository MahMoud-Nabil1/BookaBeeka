package com.system.booking.modules.booking;

import com.system.booking.modules.booking.api.BookingController;
import com.system.booking.modules.booking.api.BookingDto;
import com.system.booking.modules.booking.api.BookingModuleApi;
import com.system.booking.modules.booking.api.CancellationResultDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerStaffTest {

    @Mock
    private BookingModuleApi bookingApi;

    @InjectMocks
    private BookingController bookingController;

    private UUID tenantId;
    private UUID staffUserId;
    private UUID customerId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        staffUserId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("listMyBookings delegates to listBookingsForTenant when called by HotelUserPrincipal")
    void listMyBookings_staffUser_returnsTenantBookings() {
        HotelUserPrincipal staffPrincipal = new HotelUserPrincipal(
                staffUserId,
                "admin@hotel.com",
                "ADMIN",
                tenantId
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        staffPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        BookingDto dto = new BookingDto(
                bookingId,
                tenantId,
                customerId,
                UUID.randomUUID(),
                "Ocean Suite",
                "301",
                "Deluxe",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2),
                null,
                null,
                1,
                "PENDING_PAYMENT",
                BigDecimal.valueOf(500),
                "USD",
                null,
                null,
                0,
                OffsetDateTime.now()
        );
        when(bookingApi.listBookingsForTenant(tenantId)).thenReturn(List.of(dto));

        ResponseEntity<List<BookingDto>> response = bookingController.listMyBookings(tenantId);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).roomName()).isEqualTo("Ocean Suite");
        verify(bookingApi).listBookingsForTenant(tenantId);
    }

    @Test
    @DisplayName("listMyBookings delegates to listBookingsForCustomer when called by CustomerPrincipal")
    void listMyBookings_customerUser_returnsCustomerBookings() {
        CustomerPrincipal customerPrincipal = new CustomerPrincipal(
                customerId,
                "guest@example.com"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        customerPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                )
        );

        when(bookingApi.listBookingsForCustomer(tenantId, customerId)).thenReturn(List.of());

        ResponseEntity<List<BookingDto>> response = bookingController.listMyBookings(tenantId);

        assertThat(response.getBody()).isNotNull().isEmpty();
        verify(bookingApi).listBookingsForCustomer(tenantId, customerId);
    }

    @Test
    @DisplayName("confirmBooking by staff member resolves tenantId from principal if missing")
    void confirmBooking_staffUser_succeeds() {
        HotelUserPrincipal staffPrincipal = new HotelUserPrincipal(
                staffUserId,
                "owner@hotel.com",
                "OWNER",
                tenantId
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        staffPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
                )
        );

        ResponseEntity<Map<String, String>> response = bookingController.confirmBooking(bookingId, null);

        assertThat(response.getBody()).containsEntry("message", "Booking confirmed");
        verify(bookingApi).confirmBooking(tenantId, bookingId);
    }

    @Test
    @DisplayName("cancelBooking by staff member passes staffUserId as actorId and tenantId")
    void cancelBooking_staffUser_succeeds() {
        HotelUserPrincipal staffPrincipal = new HotelUserPrincipal(
                staffUserId,
                "owner@hotel.com",
                "OWNER",
                tenantId
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        staffPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
                )
        );

        CancellationResultDto resultDto = new CancellationResultDto(
                bookingId, BigDecimal.ZERO, 0, "CANCELLED"
        );
        when(bookingApi.cancelBooking(tenantId, bookingId, "Guest request", staffUserId))
                .thenReturn(resultDto);

        ResponseEntity<CancellationResultDto> response = bookingController.cancelBooking(
                bookingId, tenantId, "Guest request"
        );

        assertThat(response.getBody()).isEqualTo(resultDto);
        verify(bookingApi).cancelBooking(tenantId, bookingId, "Guest request", staffUserId);
    }
}
