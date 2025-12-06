package com.uit.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.entity.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class FirebaseMessagingService {
    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(List<String> deviceTokens, SendNotificationRequest request) throws FirebaseMessagingException {
//        firebaseMessaging = FirebaseMessaging.getInstance();
        Notification notification = Notification
                .builder()
                .setTitle(request.getTitle())
                .setBody(request.getContent())
                .setImage(request.getImage())
                .build();

        List<Message> messages = deviceTokens
                .stream()
                .map(token -> {
                    return Message
                            .builder()
                            .setToken(token)
                            .setNotification(notification)
//                            .putAllData(msg.get)
                            .build();
                }).toList();

        CompletableFuture.runAsync(() -> {
            try {
//                firebaseMessagingService.sendNotification(newNotification);
                firebaseMessaging.sendEach(messages);
            } catch (FirebaseMessagingException e) {
                e.printStackTrace();
//                // Optionally update notification status sent=false
            }
        });

//        return "Notification Sent Successfully";
    }
}
