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
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // Transaction Exchange - receives events from transaction-service
    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(RabbitMQConstants.TRANSACTION_EXCHANGE, true, false);
    }

    // Queue for OTP notifications (otp.generated events)
    @Bean
    public Queue otpQueue() {
        return new Queue("notification.otp.queue", true);
    }

    // Queue for transaction completion/failure notifications
    @Bean
    public Queue transactionNotificationQueue() {
        return new Queue(RabbitMQConstants.NOTIFICATION_QUEUE, true);
    }

    // Binding: OTP events -> OTP Queue
    @Bean
    public Binding otpBinding() {
        return BindingBuilder.bind(otpQueue())
                .to(transactionExchange())
                .with("otp.generated");
    }

    // Binding: Transaction notifications -> Transaction Notification Queue
    // Matches routing keys: notification.TransactionCompleted, notification.TransactionFailed, etc.
    @Bean
    public Binding transactionNotificationBinding() {
        return BindingBuilder.bind(transactionNotificationQueue())
                .to(transactionExchange())
                .with("notification.*");
    }

//    @Bean
//    public Binding notificationBinding() {
//        return BindingBuilder.bind();
//    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
