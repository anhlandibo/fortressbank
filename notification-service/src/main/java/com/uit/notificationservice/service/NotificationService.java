package com.uit.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.dto.TextBeeRequest;
import com.uit.notificationservice.entity.NotificationMessage;
import com.uit.notificationservice.repository.NotificationRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
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

    @Value("${textbee.api.key}")
    private String apiKey;

    @Value("${textbee.device.id}")
    private String deviceId;

    public void sendSmsOtp(String phoneNumber, String otpCode) {
        String url = "https://api.textbee.dev/api/v1/gateway/devices/" + deviceId + "/send-sms";

        TextBeeRequest request = new TextBeeRequest(new String[]{phoneNumber}, "Your FortressBank verification code is: " + otpCode);

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

    /**
     * Send push notification via Firebase Cloud Messaging
     */
    public void sendPushNotification(String deviceToken, String title, String body) {
        try {
            log.info("Sending push notification to token: {}", deviceToken);
            
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .deviceToken(deviceToken)
                    .title(title)
                    .content(body)
                    .type("NOTIFICATION")
                    .build();
            
            List<String> tokens = new ArrayList<>();
            tokens.add(deviceToken);
            
            firebaseMessagingService.sendNotification(tokens, request);
            log.info("Push notification sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage(), e);
        }
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

//        NotificationMessage newNotification = NotificationMessage.builder()
//                .userId("userId_123321")
//                .title("Fortress Bank Notification")
//                .content("Money transferred: " + "+" + 30000 + "\n" +
//                        "Remaining Balance: " + 530000)
//                .image(null)
//                .type("transaction")
//                .deviceToken(request.getDeviceToken())
//                .isRead(false)
//                .sentAt(new Date())
//                .createdAt(new Date())
//                .build();

//        SendNotificationRequest newNotification = new SendNotificationRequest(
//                "userId_123321",
//                "Fortress Bank Notification",
//                "Money transferred: " + "+" + 30000 + "\n" +
//                        "Remaining Balance: " + 530000,
//                null,
//                "TRANSACTION",
//                false,
//                new Date()
//        );

        List<String> tokens = new ArrayList<>();
        tokens.add(request.getDeviceToken());

        notificationRepo.save(newNotification);
        firebaseMessagingService.sendNotification(tokens, request);

        return newNotification;
    }
}
