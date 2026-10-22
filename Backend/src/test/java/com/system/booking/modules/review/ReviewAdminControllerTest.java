package com.system.booking.modules.review;

import com.system.booking.modules.review.api.ReviewAdminController;
import com.system.booking.modules.review.internal.dto.ReplyToReviewRequestDto;
import com.system.booking.modules.review.internal.dto.ReviewResponseDto;
import com.system.booking.modules.review.internal.service.ReviewReplyService;
import com.system.booking.modules.review.internal.service.ReviewService;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.security.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewAdminControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReviewReplyService reviewReplyService;

    @InjectMocks
    private ReviewAdminController controller;

    private MockedStatic<SecurityUtil> mockedSecurityUtil;
    private UUID tenantId;
    private UUID adminId;
    private HotelUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        adminPrincipal = new HotelUserPrincipal(adminId, "admin@hotel.com", "ADMIN", tenantId);

        mockedSecurityUtil = mockStatic(SecurityUtil.class);
        mockedSecurityUtil.when(SecurityUtil::getCurrentHotelUserPrincipal).thenReturn(adminPrincipal);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtil.close();
    }

    @Test
    void listReviews_withoutRoomId_callsGetReviewsForTenant() {
        ReviewResponseDto dto = new ReviewResponseDto(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(),
                "John", "Doe", null, UUID.randomUUID(), null,
                5, "Great stay!", true, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        Page<ReviewResponseDto> page = new PageImpl<>(List.of(dto));

        when(reviewService.getReviewsForTenant(eq(tenantId), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<Page<ReviewResponseDto>> response = controller.listReviews(null, null, 0, 20);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(reviewService).getReviewsForTenant(eq(tenantId), any(PageRequest.class));
        verify(reviewService, never()).getReviewsForRoom(any(), any(), any());
    }

    @Test
    void listReviews_withRoomId_callsGetReviewsForRoom() {
        UUID roomId = UUID.randomUUID();
        ReviewResponseDto dto = new ReviewResponseDto(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(),
                "Jane", "Smith", null, roomId, null,
                4, "Nice room!", true, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        Page<ReviewResponseDto> page = new PageImpl<>(List.of(dto));

        when(reviewService.getReviewsForRoom(eq(tenantId), eq(roomId), any(PageRequest.class)))
                .thenReturn(page);

        ResponseEntity<Page<ReviewResponseDto>> response = controller.listReviews(null, roomId, 0, 20);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(roomId, response.getBody().getContent().get(0).roomId());
        verify(reviewService).getReviewsForRoom(eq(tenantId), eq(roomId), any(PageRequest.class));
        verify(reviewService, never()).getReviewsForTenant(any(), any());
    }

    @Test
    void replyToReview_byAdmin_callsReviewReplyService() {
        UUID reviewId = UUID.randomUUID();
        String replyText = "Thank you for staying with us!";
        ReplyToReviewRequestDto request = new ReplyToReviewRequestDto(replyText);

        ReviewResponseDto dto = new ReviewResponseDto(
                reviewId, tenantId, UUID.randomUUID(), UUID.randomUUID(),
                "John", "Doe", null, UUID.randomUUID(), null,
                5, "Great stay!", true, replyText, adminId, "ADMIN", LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(reviewReplyService.replyToReview(eq(reviewId), eq(tenantId), eq(adminId), eq("ADMIN"), eq(replyText)))
                .thenReturn(dto);

        ResponseEntity<ReviewResponseDto> response = controller.replyToReview(reviewId, null, request);

        assertNotNull(response.getBody());
        assertEquals(replyText, response.getBody().reply());
        assertEquals("ADMIN", response.getBody().repliedByRole());
        verify(reviewReplyService).replyToReview(eq(reviewId), eq(tenantId), eq(adminId), eq("ADMIN"), eq(replyText));
    }
}
