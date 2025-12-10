package com.uit.sharedkernel.audit;

import com.uit.sharedkernel.amqp.RabbitMQMessageProducer;
import com.uit.sharedkernel.constants.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Helper component to publish audit events directly to RabbitMQ
 * Used by all microservices to log audit trails
 * 
 * Note: Audit events are sent directly (not via Outbox) since they are non-critical.
 * If an audit log is lost, it won't affect business operations.
 * Use Outbox pattern only for critical business events (transactions, transfers, etc.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final RabbitMQMessageProducer messageProducer;

    /**
     * Publish an audit event directly to RabbitMQ
     * 
     * @param auditEvent The audit event to publish
     */
    public void publishAuditEvent(AuditEventDto auditEvent) {
        try {
            // Set timestamp if not provided
            if (auditEvent.getTimestamp() == null) {
                auditEvent.setTimestamp(LocalDateTime.now());
            }

            // Publish directly to RabbitMQ
            messageProducer.publish(
                auditEvent,
                RabbitMQConstants.AUDIT_EXCHANGE,
                RabbitMQConstants.AUDIT_LOG
            );
            
            log.debug("Audit event published: service={}, entityType={}, entityId={}, action={}",
                    auditEvent.getServiceName(), auditEvent.getEntityType(), 
                    auditEvent.getEntityId(), auditEvent.getAction());

        } catch (Exception e) {
            log.warn("Failed to publish audit event (non-critical): service={}, entityType={}, action={}",
                    auditEvent.getServiceName(), auditEvent.getEntityType(), auditEvent.getAction(), e);
            // Don't throw exception - audit logging failures should not break business logic
        }
    }

    /**
     * Convenience method to publish audit event with minimal parameters
     */
    public void logAudit(String serviceName, String entityType, String entityId, 
                         String action, String userId, String result) {
        AuditEventDto auditEvent = AuditEventDto.builder()
                .serviceName(serviceName)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .userId(userId)
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
        
        publishAuditEvent(auditEvent);
    }
}
