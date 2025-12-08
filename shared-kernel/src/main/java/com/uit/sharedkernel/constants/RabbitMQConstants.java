package com.uit.sharedkernel.constants;

public class RabbitMQConstants {
    // Exchange
    public static final String TRANSACTION_EXCHANGE = "transaction-exchange";

    // Routing Keys
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String MONEY_TRANSFERRED = "money.transferred";

    // Queues
    public static final String NOTIFICATION_QUEUE = "notification-queue";

    private RabbitMQConstants() {
        // Prevent instantiation
    }
}
