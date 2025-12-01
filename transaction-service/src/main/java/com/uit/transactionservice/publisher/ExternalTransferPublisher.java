package com.uit.transactionservice.publisher;

import com.uit.transactionservice.config.ExternalTransferRabbitMQConfig;
import com.uit.transactionservice.event.ExternalTransferInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for external transfer events to RabbitMQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTransferPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish external transfer initiation event to RabbitMQ.
     * External bank mock will consume this message and process asynchronously.
     */
    public void publishExternalTransferInitiated(ExternalTransferInitiatedEvent event) {
        try {
            log.info("Publishing external transfer event to MQ - TxID: {} - To Bank: {} - Amount: {}",
                    event.getTransactionId(),
                    event.getDestinationBankCode(),
                    event.getAmount());

            rabbitTemplate.convertAndSend(
                    ExternalTransferRabbitMQConfig.EXTERNAL_TRANSFER_EXCHANGE,
                    ExternalTransferRabbitMQConfig.EXTERNAL_TRANSFER_INITIATE_ROUTING_KEY,
                    event
            );

            log.info("External transfer event published successfully - TxID: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to publish external transfer event - TxID: {}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to publish external transfer event", e);
        }
    }
}
