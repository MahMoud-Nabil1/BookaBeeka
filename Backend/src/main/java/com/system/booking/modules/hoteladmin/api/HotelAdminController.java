package com.system.booking.modules.hoteladmin.api;

import com.system.booking.modules.hoteladmin.internal.dto.HotelAdminProfileResponse;
import com.system.booking.modules.hoteladmin.internal.dto.UpdateHotelAdminProfileRequest;
import com.system.booking.modules.hoteladmin.internal.service.HotelAdminService;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class HotelAdminController {

    private final HotelAdminService hotelAdminService;

    @GetMapping("/profile")
    public ResponseEntity<HotelAdminProfileResponse> getProfile(
            @AuthenticationPrincipal HotelUserPrincipal principal) {
        return ResponseEntity.ok(hotelAdminService.getProfile(principal.id(), principal.tenantId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<HotelAdminProfileResponse> updateProfile(
            @AuthenticationPrincipal HotelUserPrincipal principal,
            @Valid @RequestBody UpdateHotelAdminProfileRequest request) {
        return ResponseEntity.ok(hotelAdminService.updateProfile(principal.id(), principal.tenantId(), request));
    }
}