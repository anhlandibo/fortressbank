package com.uit.notificationservice.config;

import com.uit.sharedkernel.constants.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(RabbitMQConstants.TRANSACTION_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(RabbitMQConstants.NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue notificationQueue, TopicExchange transactionExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(transactionExchange)
                .with(RabbitMQConstants.TRANSACTION_CREATED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
