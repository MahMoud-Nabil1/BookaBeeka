package com.system.booking.modules.review;

import com.system.booking.modules.booking.api.BookingDto;
import com.system.booking.modules.booking.api.BookingModuleApi;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import com.system.booking.modules.inventory.api.InventoryModuleApi;
import com.system.booking.modules.review.internal.dto.CreateReviewRequestDto;
import com.system.booking.modules.review.internal.dto.ReviewResponseDto;
import com.system.booking.modules.review.internal.entity.Review;
import com.system.booking.modules.review.internal.exception.BookingNotEligibleForReviewException;
import com.system.booking.modules.review.internal.exception.DuplicateReviewException;
import com.system.booking.modules.review.internal.repository.ReviewRepository;
import com.system.booking.modules.review.internal.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepo;

    @Mock
    private BookingModuleApi bookingApi;

    @Mock
    private InventoryModuleApi inventoryApi;

    @Mock
    private CustomerRepository customerRepo;

    @InjectMocks
    private ReviewService reviewService;

    private UUID tenantId;
    private UUID customerId;
    private UUID bookingId;
    private UUID roomId;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
    }

    @Test
    void createReview_success_completed_withServiceOfferingId() {
        // Booking already carries serviceOfferingId — no inventory fallback needed
        BookingDto bookingDto = new BookingDto(
                bookingId, tenantId, customerId, roomId, serviceId /* serviceOfferingId */,
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(1),
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(1),
                1, "COMPLETED", BigDecimal.valueOf(100), "USD", null, null, 1, OffsetDateTime.now().minusDays(2)
        );

        when(bookingApi.getBookingById(tenantId, bookingId)).thenReturn(bookingDto);
        when(inventoryApi.serviceOfferingExists(serviceId)).thenReturn(true);
        when(reviewRepo.existsByBookingId(bookingId)).thenReturn(false);
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        // serviceId is no longer part of the request — the backend derives it from the booking
        CreateReviewRequestDto request = new CreateReviewRequestDto(
                tenantId, bookingId, 5, "Great stay!"
        );

        ReviewResponseDto response = reviewService.createReview(request, customerId);

        assertNotNull(response);
        assertEquals(5, response.rating());
        assertEquals("Great stay!", response.comment());
        assertEquals(bookingId, response.bookingId());
        assertEquals(serviceId, response.serviceId()); // derived from bookingDto.serviceOfferingId()

        // inventory fallback should NOT have been called when serviceOfferingId is present
        verify(inventoryApi, never()).getFirstServiceOfferingIdForRoom(any(), any());
    }

    @Test
    void createReview_success_completed_nullServiceOfferingId_fallsBackToRoom() {
        // Room-only booking (serviceOfferingId = null) — backend must look up via room link
        BookingDto bookingDto = new BookingDto(
                bookingId, tenantId, customerId, roomId, null /* no serviceOfferingId */,
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(1),
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(1),
                1, "COMPLETED", BigDecimal.valueOf(100), "USD", null, null, 1, OffsetDateTime.now().minusDays(2)
        );

        when(bookingApi.getBookingById(tenantId, bookingId)).thenReturn(bookingDto);
        when(reviewRepo.existsByBookingId(bookingId)).thenReturn(false);
        when(inventoryApi.getFirstServiceOfferingIdForRoom(tenantId, roomId)).thenReturn(serviceId);
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateReviewRequestDto request = new CreateReviewRequestDto(
                tenantId, bookingId, 4, "Nice room!"
        );

        ReviewResponseDto response = reviewService.createReview(request, customerId);

        assertNotNull(response);
        assertEquals(4, response.rating());
        // serviceId resolved via room-link fallback
        assertEquals(serviceId, response.serviceId());
        verify(inventoryApi).getFirstServiceOfferingIdForRoom(tenantId, roomId);
    }

    @Test
    void createReview_success_confirmed_and_started() {
        BookingDto bookingDto = new BookingDto(
                bookingId, tenantId, customerId, roomId, serviceId /* serviceOfferingId */,
                OffsetDateTime.now().minusHours(2), OffsetDateTime.now().plusHours(2),
                LocalDate.now(), LocalDate.now().plusDays(1),
                1, "CONFIRMED", BigDecimal.valueOf(100), "USD", null, null, 1, OffsetDateTime.now().minusHours(2)
        );

        when(bookingApi.getBookingById(tenantId, bookingId)).thenReturn(bookingDto);
        when(inventoryApi.serviceOfferingExists(serviceId)).thenReturn(true);
        when(reviewRepo.existsByBookingId(bookingId)).thenReturn(false);
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        // serviceId is no longer part of the request — the backend derives it from the booking
        CreateReviewRequestDto request = new CreateReviewRequestDto(
                tenantId, bookingId, 4, "Nice!"
        );

        ReviewResponseDto response = reviewService.createReview(request, customerId);

        assertNotNull(response);
        assertEquals(4, response.rating());
        verify(inventoryApi, never()).getFirstServiceOfferingIdForRoom(any(), any());
    }
}
