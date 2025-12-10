package com.uit.sharedkernel.constants;

public class RabbitMQConstants {
    // Exchanges
    public static final String TRANSACTION_EXCHANGE = "transaction-exchange";
    public static final String AUDIT_EXCHANGE = "audit.exchange";
    
    // Routing Keys
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String MONEY_TRANSFERRED = "money.transferred";
    public static final String AUDIT_LOG = "audit.log";
    
    // Queues
    public static final String NOTIFICATION_QUEUE = "notification-queue";
    public static final String AUDIT_QUEUE = "audit.queue";
    
    private RabbitMQConstants() {
        // Prevent instantiation
    }
}
