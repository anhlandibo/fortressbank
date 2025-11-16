package com.uit.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.uit.notificationservice.entity.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FirebaseMessagingService {
    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(NotificationMessage msg) throws FirebaseMessagingException {
//        firebaseMessaging = FirebaseMessaging.getInstance();
        Notification notification = Notification
                .builder()
                .setTitle(msg.getTitle())
                .setBody(msg.getContent())
                .setImage(msg.getImage())
                .build();

        Message message = Message
                .builder()
                .setToken(msg.getDeviceToken())
                .setNotification(notification)
//                .putAllData(msg.get)
                .build();


        firebaseMessaging.send(message);
//        return "Notification Sent Successfully";
    }
}
