package com.uit.notificationservice.listener;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.uit.notificationservice.dto.SendNotificationRequest;
import com.uit.notificationservice.entity.UserPreference;
import com.uit.notificationservice.repository.UserPreferenceRepo;
import com.uit.notificationservice.service.FirebaseMessagingService;
import com.uit.notificationservice.service.NotificationService;
import com.uit.sharedkernel.constants.RabbitMQConstants;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {
    private final NotificationService notificationService;
    private final FirebaseMessagingService firebaseMessagingService;
    private final UserPreferenceRepo userPreferenceRepo;

    /**
     * Handles OTP generation events from transaction-service
     * Queue: notification.otp.queue
     * Routing Key: otp.generated
     * Purpose: Send OTP via SMS to user's phone number
     */
    @RabbitListener(queues = "notification.otp.queue")
    public void handleOtpNotification(Map<String, Object> message) {
        log.info("Received OTP notification event: {}", message);

        try {
            String transactionId = (String) message.get("transactionId");
            String phoneNumber = (String) message.get("phoneNumber");
            String otpCode = (String) message.get("otpCode");

            log.info("Processing OTP notification for transaction: {}, phone: {}", transactionId, phoneNumber);

            // Send SMS OTP via TextBee API
            notificationService.sendSmsOtp(phoneNumber, otpCode);

            log.info("OTP SMS sent successfully for transaction: {}", transactionId);

        } catch (Exception e) {
            log.error("Failed to send OTP notification: {}", e.getMessage(), e);
            // OTP failures are critical - consider implementing retry logic or alerting
        }
    }

    /**
     * Handles transaction completion/failure events from transaction-service
     * Queue: notification-queue
     * Routing Keys: notification.TransactionCompleted, notification.TransactionFailed, notification.ExternalTransferInitiated
     * Purpose: Send multi-channel notifications (Push, SMS, Email) based on user preferences
     */
    @RabbitListener(queues = RabbitMQConstants.NOTIFICATION_QUEUE)
    public void handleTransactionNotification(Map<String, Object> message) {
        log.info("Received transaction notification event: {}", message);

        try {
            // Extract transaction details from message payload
            String transactionId = (String) message.get("transactionId");
            String senderAccountId = (String) message.get("senderAccountId");
            String receiverAccountId = (String) message.get("receiverAccountId");
            Object amountObj = message.get("amount");
            String status = (String) message.get("status");
            Boolean success = (Boolean) message.get("success");
            String notificationMessage = (String) message.get("message");
            String timestamp = (String) message.get("timestamp");

            // Convert amount to BigDecimal
            BigDecimal amount = null;
            if (amountObj instanceof Number) {
                amount = new BigDecimal(amountObj.toString());
            }

            log.info("Processing transaction notification - ID: {}, Status: {}, Success: {}", 
                    transactionId, status, success);

            // Get user preferences for sender account (primary notification recipient)
            UserPreference senderPreference = userPreferenceRepo.findById(senderAccountId)
                    .orElseGet(() -> {
                        log.warn("User preference not found for sender: {}, using defaults", senderAccountId);
                        return createDefaultPreference(senderAccountId);
                    });

            // Build notification title and content
            String title = success ? "Transaction Successful" : "Transaction Failed";
            String content = formatTransactionNotification(success, amount, status, notificationMessage);

            // Send Push Notification (if enabled and device tokens exist)
            // Modern banking trend: Use push notifications instead of SMS for transaction alerts
            // Benefits: Free, real-time, rich content, no SMS costs
            if (senderPreference.isPushNotificationEnabled() && 
                senderPreference.getDeviceTokens() != null && 
                !senderPreference.getDeviceTokens().isEmpty()) {
                
                try {
                    SendNotificationRequest pushRequest = new SendNotificationRequest(
                            senderAccountId,
                            title,
                            content,
                            null, // image
                            "TRANSACTION",
                            false, // isRead
                            new Date()
                    );

                    firebaseMessagingService.sendNotification(
                            senderPreference.getDeviceTokens(), 
                            pushRequest
                    );
                    log.info("Push notification sent for transaction: {} to {} devices", 
                            transactionId, senderPreference.getDeviceTokens().size());
                } catch (Exception e) {
                    log.error("Failed to send push notification for transaction {}: {}", 
                            transactionId, e.getMessage());
                }
            } else {
                log.info("Push notification not sent for transaction: {} - " +
                        "Push disabled or no device tokens registered", transactionId);
            }

            // Note: SMS and Email notifications removed for transaction alerts
            // SMS is only used for critical OTP verification (security requirement)
            // Email notifications can be added later for monthly statements/reports

            log.info("Transaction notification processing completed for: {}", transactionId);

        } catch (Exception e) {
            log.error("Critical error processing transaction notification: {}", e.getMessage(), e);
            // Don't throw exception - notifications are non-critical, avoid message requeue loops
        }
    }

    /**
     * Format transaction notification message
     */
    private String formatTransactionNotification(Boolean success, BigDecimal amount, 
                                                  String status, String message) {
        StringBuilder sb = new StringBuilder();
        
        if (success) {
            sb.append("Your transaction has been completed successfully.\n");
        } else {
            sb.append("Your transaction has failed.\n");
        }
        
        if (amount != null) {
            sb.append("Amount: ").append(amount).append("\n");
        }
        
        sb.append("Status: ").append(status).append("\n");
        
        if (message != null && !message.isEmpty()) {
            sb.append("Details: ").append(message);
        }
        
        return sb.toString();
    }

    /**
     * Create default user preference if not found
     * Saves to database for future use
     */
    private UserPreference createDefaultPreference(String userId) {
        log.info("Creating and saving default preference for user: {}", userId);
        
        UserPreference defaultPref = new UserPreference();
        defaultPref.setUserId(userId);
        defaultPref.setPushNotificationEnabled(true);
        defaultPref.setSmsNotificationEnabled(false);
        defaultPref.setEmailNotificationEnabled(false);
        
        // Save to database so it persists
        return userPreferenceRepo.save(defaultPref);
    }
}
