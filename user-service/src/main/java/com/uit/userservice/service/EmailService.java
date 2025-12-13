package com.uit.userservice.service;

public interface EmailService {
    /**
     * Send OTP via email
     * @param toEmail recipient email address
     * @param otp 6-digit OTP code
     * @param expiryMinutes OTP expiry time in minutes
     */
    void sendOtpEmail(String toEmail, String otp, int expiryMinutes);

    /**
     * Send welcome email after successful registration (optional)
     * @param toEmail recipient email address
     * @param fullName user's full name
     */
    void sendWelcomeEmail(String toEmail, String fullName);
}
