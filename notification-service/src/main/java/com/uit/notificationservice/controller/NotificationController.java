package com.uit.notificationservice.controller;

import com.uit.notificationservice.dto.SendSmsOtpRequest;
import com.uit.notificationservice.service.NotificationService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/sms/send-otp")
    public ResponseEntity<ApiResponse<String>> sendSmsOtp(@RequestBody SendSmsOtpRequest request) {
        notificationService.sendSmsOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success("SMS OTP sent successfully."));
    }
}
