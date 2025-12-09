package com.uit.externalbankmock.listener;

import com.uit.externalbankmock.config.RabbitMQConfig;
import com.uit.externalbankmock.entity.ExternalTransfer;
import com.uit.externalbankmock.entity.TransferStatus;
import com.uit.externalbankmock.event.ExternalTransferInitiatedEvent;
import com.uit.externalbankmock.repository.ExternalTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for external transfer initiation events from RabbitMQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTransferListener {

    private final ExternalTransferRepository transferRepository;

    /**
     * Consume external transfer initiation events from FortressBank.
     * Store in database and process asynchronously via scheduled job.
     */
    @RabbitListener(queues = RabbitMQConfig.EXTERNAL_TRANSFER_INITIATE_QUEUE)
    @Transactional
    public void handleExternalTransferInitiated(ExternalTransferInitiatedEvent event) {
        log.info("Received external transfer event from MQ - TxID: {} - Amount: {} - To Bank: {}",
                event.getTransactionId(),
                event.getAmount(),
                event.getDestinationBankCode());

        try {
            // Check idempotency - if already exists, skip
            var existing = transferRepository.findByFortressBankTransactionId(event.getTransactionId());
            if (existing.isPresent()) {
                log.warn("Transfer already exists - TxID: {} - Skipping duplicate", event.getTransactionId());
                return;
            }

            // Create pending transfer
            ExternalTransfer transfer = ExternalTransfer.builder()
                    .fortressBankTransactionId(event.getTransactionId())
                    .sourceAccountNumber(event.getSourceAccountNumber())
                    .sourceBankCode(event.getSourceBankCode())
                    .destinationAccountNumber(event.getDestinationAccountNumber())
                    .destinationBankCode(event.getDestinationBankCode())
                    .amount(event.getAmount())
                    .description(event.getDescription())
                    .status(TransferStatus.PENDING)
                    .message("Transfer request received from message queue, pending processing")
                    .build();

            transfer = transferRepository.save(transfer);
            log.info("External transfer saved from MQ - External TxID: {} - Status: PENDING", transfer.getId());

        } catch (Exception e) {
            log.error("Error processing external transfer event from MQ - TxID: {}", 
                    event.getTransactionId(), e);
            // Exception will cause message to be requeued or sent to DLQ
            throw new RuntimeException("Failed to process external transfer event", e);
        }
    }
}
