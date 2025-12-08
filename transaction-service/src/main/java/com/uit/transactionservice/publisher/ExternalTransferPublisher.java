package com.uit.transactionservice.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.sharedkernel.outbox.OutboxEvent;
import com.uit.sharedkernel.outbox.OutboxEventStatus;
import com.uit.sharedkernel.outbox.repository.OutboxEventRepository;
import com.uit.transactionservice.config.ExternalTransferRabbitMQConfig;
import com.uit.transactionservice.event.ExternalTransferInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Creates an outbox event for an external transfer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTransferPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates an outbox event for an external transfer initiation.
     * The OutboxScheduler will publish this message asynchronously.
     */
    public void publishExternalTransferInitiated(ExternalTransferInitiatedEvent event) {
        try {
            log.info("Creating outbox event for external transfer - TxID: {}", event.getTransactionId());

            String payloadJson = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Transaction")
                    .aggregateId(event.getTransactionId())
                    .eventType(event.getClass().getSimpleName())
                    .exchange(ExternalTransferRabbitMQConfig.EXTERNAL_TRANSFER_EXCHANGE)
                    .routingKey(ExternalTransferRabbitMQConfig.EXTERNAL_TRANSFER_INITIATE_ROUTING_KEY)
                    .payload(payloadJson)
                    .status(OutboxEventStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);

            log.info("External transfer outbox event created successfully - TxID: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to create external transfer outbox event - TxID: {}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to create external transfer outbox event", e);
        }
    }
}
