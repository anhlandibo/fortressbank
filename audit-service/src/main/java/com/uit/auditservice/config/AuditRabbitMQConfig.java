package com.uit.auditservice.config;

import com.uit.sharedkernel.constants.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditRabbitMQConfig {

    @Value("${rabbitmq.exchange.audit:" + RabbitMQConstants.AUDIT_EXCHANGE + "}")
    private String auditExchange;

    @Value("${rabbitmq.queue.audit:" + RabbitMQConstants.AUDIT_QUEUE + "}")
    private String auditQueue;

    @Value("${rabbitmq.routing-key.audit:" + RabbitMQConstants.AUDIT_LOG + "}")
    private String auditRoutingKey;

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(auditQueue)
                .withArgument("x-message-ttl", 86400000) // 24 hours TTL
                .build();
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(auditExchange);
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {
        return BindingBuilder
                .bind(auditQueue)
                .to(auditExchange)
                .with(auditRoutingKey);
    }
}
