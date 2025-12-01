package com.uit.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.dto.TextBeeRequest;
import com.uit.notificationservice.entity.NotificationMessage;
import com.uit.notificationservice.repository.NotificationRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final WebClient.Builder webClientBuilder;
    private final NotificationRepo notificationRepo;
    private final FirebaseMessagingService firebaseMessagingService;

    @Value("${textbee.api.key}" )
    private String apiKey;

    @Value("${textbee.device.id}" )
    private String deviceId;

    public void sendSmsOtp(String phoneNumber, String otpCode) {
        String url = "https://api.textbee.dev/api/v1/gateway/devices/" + deviceId + "/send-sms";

        TextBeeRequest request = new TextBeeRequest(new String[]{"0857311444"}, "Your FortressBank verification code is: " + otpCode);

        webClientBuilder.build()
                .post()
                .uri(url)
                .header("x-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        success -> log.info("SMS OTP sent successfully to {}", phoneNumber),
                        error -> log.error("Failed to send SMS OTP to {}: {}", phoneNumber, error.getMessage())
                );
    }

    public List<NotificationMessage> getNotifications() {
        return notificationRepo.findAll();
    }

    public NotificationMessage createAndSendNotification(SendNotificationRequest request) throws FirebaseMessagingException {
        NotificationMessage newNotification = NotificationMessage.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .content(request.getContent())
                .image(request.getImage())
                .type(request.getType())
                .deviceToken(request.getDeviceToken())
                .isRead(false)
                .sentAt(new Date())
                .createdAt(new Date())
                .build();

        notificationRepo.save(newNotification);
        CompletableFuture.runAsync(() -> {
            try {
                firebaseMessagingService.sendNotification(newNotification);
            } catch (FirebaseMessagingException e) {
                e.printStackTrace();
                // Optionally update notification status sent=false
            }
        });

//        firebaseMessagingService.sendNotification(newNotification);

//        SendNotificationDto dto = No
        return newNotification;
    }
}
