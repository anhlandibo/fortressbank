package com.uit.sharedkernel.constants;

public class RabbitMQConstants {
    // Exchange
    public static final String TRANSACTION_EXCHANGE = "transaction-exchange";
    
    // Routing Keys
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String MONEY_TRANSFERRED = "money.transferred";
    
    // Queues
    public static final String NOTIFICATION_QUEUE = "notification-queue";

    public static final String INTERNAL_EXCHANGE = "internal.exchange";
    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String USER_CREATED_ROUTING_KEY = "user.created";

    public static final String NOTIFICATION_TRANSFER_QUEUE = "notification.transfer.queue";

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    public static final String NOTIF_TRANSFER_CREATED_KEY = "notification.transfer.created";
    private RabbitMQConstants() {
        // Prevent instantiation
    }
}
