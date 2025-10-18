package com.uit.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void sendSmsOtp(String phoneNumber, String otpCode) {
        // TODO: Implement the actual SMS sending logic
        log.info("Sending OTP {} to {}", otpCode, phoneNumber);
    }
}
