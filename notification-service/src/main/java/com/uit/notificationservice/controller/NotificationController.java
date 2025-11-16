package com.uit.notificationservice.controller;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.dto.SendNotificationResponse;
import com.uit.notificationservice.dto.SendSmsOtpRequest;
import com.uit.notificationservice.entity.NotificationMessage;
import com.uit.notificationservice.entity.UserPreference;
import com.uit.notificationservice.mapper.NotificationMessageMapper;
import com.uit.notificationservice.service.NotificationService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationMessageMapper mapper;
    private final NotificationService notificationService;

    @PostMapping("/sms/send-otp")
    public ResponseEntity<ApiResponse<String>> sendSmsOtp(@RequestBody SendSmsOtpRequest request) {
        notificationService.sendSmsOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success("SMS OTP sent successfully."));
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<SendNotificationResponse>>> getNotifications() {
        List<NotificationMessage> notifications = notificationService.getNotifications();

        List<SendNotificationResponse> responses = mapper.toResponseDto(notifications);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<List<UserPreference>>> getNotificationsPreferences() {
        List<UserPreference> userPreferences = new ArrayList<>();
        return ResponseEntity.ok(ApiResponse.success(userPreferences));
    }

//    @PutMapping("/preferences")
//    public ResponseEntity<ApiResponse<List<NotificationMessage>>> updateNotificationsPreferences(@RequestBody List<UserPreference> userPreferences) {
//
//    }

    @PostMapping("/")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotification(@RequestBody SendNotificationRequest request) throws FirebaseMessagingException {
        NotificationMessage notification = notificationService.createAndSendNotification(request);

        SendNotificationResponse responseResult = mapper.toResponseDto(notification);
        return ResponseEntity.ok(ApiResponse.success(responseResult));
    }
}
