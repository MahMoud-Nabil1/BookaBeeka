package com.system.booking.modules.security.dto.response;

/**
 * Response payload returned upon successful OTP verification.
 */
public record OtpVerificationResponse(
        boolean verified,
        String message,
        String verificationToken
) {
    public static OtpVerificationResponse success(String message) {
        return new OtpVerificationResponse(true, message, null);
    }

    public static OtpVerificationResponse success(String message, String verificationToken) {
        return new OtpVerificationResponse(true, message, verificationToken);
    }
}
