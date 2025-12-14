package com.uit.accountservice.config;

import com.uit.sharedkernel.constants.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("accountRabbitConfig")
public class RabbitMQConfig {

    public static final String QUEUE = RabbitMQConstants.NOTIFICATION_TRANSFER_QUEUE;
    public static final String EXCHANGE = RabbitMQConstants.NOTIFICATION_EXCHANGE;
    public static final String NOTIF_TRANSFER_CREATED_KEY = RabbitMQConstants.NOTIF_TRANSFER_CREATED_KEY;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    @Bean
    public Binding bindingBalanceFluctuation() {
        return BindingBuilder.bind(queue()).to(topicExchange()).with(NOTIF_TRANSFER_CREATED_KEY);
    }
}
