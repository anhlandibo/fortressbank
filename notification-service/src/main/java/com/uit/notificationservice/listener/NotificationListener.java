package com.uit.notificationservice.listener;

import com.uit.sharedkernel.constants.RabbitMQConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class NotificationListener {

    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_QUEUE)
    public void handleTransactionNotification(Map<String, Object> message) {
        log.info("Received transaction notification: {}", message);
        
        // TODO: Implement notification logic
        // For now, just log the message
        // Future implementation: Send email, SMS, push notification, etc.
        
        try {
            // Extract transaction details from the message
            String eventType = (String) message.get("eventType");
            String aggregateId = (String) message.get("aggregateId");
            
            log.info("Processing notification for event type: {}, aggregate ID: {}", eventType, aggregateId);
            
            // Add your notification logic here
            // e.g., sendEmail(message), sendSMS(message), sendPushNotification(message)
            
        } catch (Exception e) {
            log.error("Error processing transaction notification: {}", e.getMessage(), e);
            // Consider implementing retry logic or dead letter queue
        }
    }
}
