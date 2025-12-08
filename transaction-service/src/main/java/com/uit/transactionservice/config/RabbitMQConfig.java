package com.uit.transactionservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("transactionRabbitConfig")
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "transaction-exchange";
    public static final String ROUTING_KEY = "transaction.created";
    
    // Queues for different services
    public static final String ACCOUNT_QUEUE = "account-service.transaction-events";
    public static final String NOTIFICATION_QUEUE = "notification-service.transaction-events";

    /**
     * Declare the main exchange (Topic Exchange for flexible routing)
     */
    @Bean
    public TopicExchange transactionExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    /**
     * Account Service Queue
     */
    @Bean
    public Queue accountQueue() {
        return QueueBuilder
                .durable(ACCOUNT_QUEUE)
                .build();
    }

    /**
     * Notification Service Queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(NOTIFICATION_QUEUE)
                .build();
    }


        /**
         * Bind Account Queue to Exchange
         */
        @Bean
        public Binding accountBinding(@org.springframework.beans.factory.annotation.Qualifier("accountQueue") Queue accountQueue,
                      @org.springframework.beans.factory.annotation.Qualifier("transactionExchange") TopicExchange transactionExchange) {
        return BindingBuilder
            .bind(accountQueue)
            .to(transactionExchange)
            .with("transaction.*");
        }

        /**
         * Bind Notification Queue to Exchange
         */
        @Bean
        public Binding notificationBinding(@org.springframework.beans.factory.annotation.Qualifier("notificationQueue") Queue notificationQueue,
                       @org.springframework.beans.factory.annotation.Qualifier("transactionExchange") TopicExchange transactionExchange) {
        return BindingBuilder
            .bind(notificationQueue)
            .to(transactionExchange)
            .with("transaction.*");
        }

    /**
     * JSON Message Converter
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
