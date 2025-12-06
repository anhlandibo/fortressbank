package com.uit.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.entity.NotificationMessage;
import com.uit.notificationservice.entity.UserPreference;
import com.uit.notificationservice.repository.UserPreferenceRepo;
import com.uit.sharedkernel.constants.RabbitMQConstants;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class NotificationListener {
    private final NotificationService notificationService;
    private final FirebaseMessagingService firebaseMessagingService;
    private final UserPreferenceRepo userPreferenceRepo;
    private final RabbitTemplate rabbitTemplate;
//    private final RabbitMQConstants rabbitMQConstants;

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_TRANSFER_QUEUE)
    public void handleTransferNotification(String userId, BigDecimal balance, BigDecimal amount, String variation) throws FirebaseMessagingException {
        UserPreference userPreference = userPreferenceRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "User Preference not found"));

        String operator = variation.equals("INCREASE") ? "+" : "-";

        SendNotificationRequest request = new SendNotificationRequest(
                userId,
                "Fortress Bank Notification",
                "Money transferred: " + operator + amount + "\n" +
                        "Remaining Balance: " + balance,
                null,
                "TRANSACTION",
                false,
                new Date()
        );

        firebaseMessagingService.sendNotification(userPreference.getDeviceTokens(), request);
    }
}
