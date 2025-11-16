package com.uit.notificationservice.config;

import com.uit.sharedkernel.constants.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String QUEUE = "notification.queue";
    private static final String EXCHANGE = RabbitMQConstants.NOTIFICATION_EXCHANGE_NAME;
    private static final String MONEY_TRANSFERRED = RabbitMQConstants.MONEY_TRANSFERRED;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    @Bean
    public Binding bindingMoneyTransferred() {
        return BindingBuilder.bind(queue()).to(topicExchange()).with(MONEY_TRANSFERRED);
    }
}
