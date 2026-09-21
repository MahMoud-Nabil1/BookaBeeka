package com.system.booking.modules.security;

import com.system.booking.modules.security.controller.AuthenticationController;
import com.system.booking.modules.security.dto.request.OtpRequest;
import com.system.booking.modules.security.dto.request.OtpVerificationRequest;
import com.system.booking.modules.security.dto.request.PasswordResetRequest;
import com.system.booking.modules.security.dto.request.ResetPasswordRequest;
import com.system.booking.modules.security.dto.response.OtpVerificationResponse;
import com.system.booking.modules.security.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @Test
    @DisplayName("POST /api/auth/otp/request returns 200 OK and dispatches message")
    void testOtpRequest() {
        OtpRequest request = new OtpRequest("user@example.com", UUID.randomUUID());
        doNothing().when(authenticationService).requestOtp(any(OtpRequest.class));

        ResponseEntity<Map<String, String>> response = authenticationController.requestOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains("verification code has been dispatched");
        verify(authenticationService).requestOtp(request);
    }

    @Test
    @DisplayName("POST /api/auth/otp/verify returns 200 OK with verification response")
    void testOtpVerify() {
        OtpVerificationRequest request = new OtpVerificationRequest("user@example.com", "123456", UUID.randomUUID());
        when(authenticationService.verifyOtp(any(OtpVerificationRequest.class)))
                .thenReturn(OtpVerificationResponse.success("OTP verified successfully"));

        ResponseEntity<OtpVerificationResponse> response = authenticationController.verifyOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().verified()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("OTP verified successfully");
        verify(authenticationService).verifyOtp(request);
    }

    @Test
    @DisplayName("POST /api/auth/password/forgot returns 200 OK and dispatches reset link")
    void testForgotPassword() {
        PasswordResetRequest request = new PasswordResetRequest("user@example.com", UUID.randomUUID());
        doNothing().when(authenticationService).requestPasswordReset(any(PasswordResetRequest.class));

        ResponseEntity<Map<String, String>> response = authenticationController.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains("password reset link has been dispatched");
        verify(authenticationService).requestPasswordReset(request);
    }

    @Test
    @DisplayName("POST /api/auth/password/reset returns 200 OK and success confirmation")
    void testResetPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewSecurePass123!");
        doNothing().when(authenticationService).resetPassword(any(ResetPasswordRequest.class));

        ResponseEntity<Map<String, String>> response = authenticationController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains("Password has been successfully reset");
        verify(authenticationService).resetPassword(request);
    }
}
