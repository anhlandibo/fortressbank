package com.uit.externalbankmock.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for external bank mock
 */
@Configuration
public class RabbitMQConfig {

    // Same exchange as transaction-service
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
     * Queue for receiving initiate requests (consumed by external-bank-mock)
     */
    @Bean
    public Queue externalTransferInitiateQueue() {
        return QueueBuilder.durable(EXTERNAL_TRANSFER_INITIATE_QUEUE).build();
    }

    /**
     * Queue for sending callbacks (produced by external-bank-mock)
     */
    @Bean
    public Queue externalTransferCallbackQueue() {
        return QueueBuilder.durable(EXTERNAL_TRANSFER_CALLBACK_QUEUE).build();
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

    /**
     * JSON message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
