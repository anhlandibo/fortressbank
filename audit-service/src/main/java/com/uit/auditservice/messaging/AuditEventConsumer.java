package com.uit.auditservice.messaging;

import com.rabbitmq.client.Channel;
import com.uit.sharedkernel.audit.AuditEventDto;
import com.uit.auditservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;

    @RabbitListener(
        queues = "${rabbitmq.queue.audit:audit.queue}",
        containerFactory = "simpleRabbitListenerContainerFactory"
    )
    public void consumeAuditEvent(AuditEventDto event, Channel channel, Message message) throws IOException {
        log.info("Received audit event from RabbitMQ: service={}, entityType={}, action={}",
                event.getServiceName(), event.getEntityType(), event.getAction());

        try {
            auditService.logAuditEvent(event);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("Successfully processed and ACKed audit event for entity: {}", event.getEntityId());
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", event, e);
            // Reject and don't requeue to prevent infinite retry loops
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            // Message will be sent to DLQ if configured, or discarded
        }
    }
}
