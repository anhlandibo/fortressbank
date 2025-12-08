package com.uit.transactionservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for external bank transfers
 */
@Configuration
public class ExternalTransferRabbitMQConfig {

    // Exchange names
    public static final String EXTERNAL_TRANSFER_EXCHANGE = "external.transfer.exchange";
    
    // Queue names
    public static final String EXTERNAL_TRANSFER_INITIATE_QUEUE = "external.transfer.initiate.queue";
    public static final String EXTERNAL_TRANSFER_CALLBACK_QUEUE = "external.transfer.callback.queue";
    
    // Routing keys
    public static final String EXTERNAL_TRANSFER_INITIATE_ROUTING_KEY = "external.transfer.initiate";
    public static final String EXTERNAL_TRANSFER_CALLBACK_ROUTING_KEY = "external.transfer.callback";

    /**
     * Topic exchange for external transfers
     */
    @Bean
    public TopicExchange externalTransferExchange() {
        return new TopicExchange(EXTERNAL_TRANSFER_EXCHANGE);
    }

    /**
     * Queue for initiating external transfers (consumed by external-bank-mock)
     */
    @Bean
    public Queue externalTransferInitiateQueue() {
        return QueueBuilder.durable(EXTERNAL_TRANSFER_INITIATE_QUEUE)
                .withArgument("x-dead-letter-exchange", EXTERNAL_TRANSFER_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Queue for receiving callbacks (consumed by transaction-service)
     */
    @Bean
    public Queue externalTransferCallbackQueue() {
        return QueueBuilder.durable(EXTERNAL_TRANSFER_CALLBACK_QUEUE)
                .withArgument("x-dead-letter-exchange", EXTERNAL_TRANSFER_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Binding for initiate queue
     */
    @Bean
    public Binding externalTransferInitiateBinding() {
        return BindingBuilder
                .bind(externalTransferInitiateQueue())
                .to(externalTransferExchange())
                .with(EXTERNAL_TRANSFER_INITIATE_ROUTING_KEY);
    }

    /**
     * Binding for callback queue
     */
    @Bean
    public Binding externalTransferCallbackBinding() {
        return BindingBuilder
                .bind(externalTransferCallbackQueue())
                .to(externalTransferExchange())
                .with(EXTERNAL_TRANSFER_CALLBACK_ROUTING_KEY);
    }
}
