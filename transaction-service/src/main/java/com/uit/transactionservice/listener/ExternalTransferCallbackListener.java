package com.uit.transactionservice.listener;

import com.uit.transactionservice.config.ExternalTransferRabbitMQConfig;
import com.uit.transactionservice.entity.SagaStep;
import com.uit.transactionservice.entity.Transaction;
import com.uit.transactionservice.entity.TransactionStatus;
import com.uit.transactionservice.event.ExternalTransferCompletedEvent;
import com.uit.transactionservice.repository.TransactionRepository;
import com.uit.transactionservice.client.AccountServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Listener for external transfer callback events from RabbitMQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTransferCallbackListener {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    /**
     * Handle external transfer completion/failure callback.
     * Called when external bank finishes processing (success or failure).
     */
    @RabbitListener(queues = ExternalTransferRabbitMQConfig.EXTERNAL_TRANSFER_CALLBACK_QUEUE)
    @Transactional
    public void handleExternalTransferCallback(ExternalTransferCompletedEvent event) {
        log.info("Received external transfer callback from MQ - FortressTxID: {} - Status: {} - Message: {}",
                event.getFortressBankTransactionId(),
                event.getStatus(),
                event.getMessage());

        try {
            // Find transaction by ID
            UUID transactionId = UUID.fromString(event.getFortressBankTransactionId());
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

            // Verify it's an external transfer
            if (!transaction.isExternalTransfer()) {
                log.error("Transaction {} is not an external transfer!", transactionId);
                return;
            }

            // Process based on status
            if ("COMPLETED".equalsIgnoreCase(event.getStatus())) {
                handleExternalTransferSuccess(transaction, event);
            } else if ("FAILED".equalsIgnoreCase(event.getStatus())) {
                handleExternalTransferFailure(transaction, event);
            } else {
                log.warn("Unknown external transfer status: {} for transaction {}", event.getStatus(), transactionId);
            }

        } catch (Exception e) {
            log.error("Error processing external transfer callback - FortressTxID: {}",
                    event.getFortressBankTransactionId(), e);
            // Message will be requeued or sent to DLQ based on RabbitMQ config
            throw new RuntimeException("Failed to process callback", e);
        }
    }

    /**
     * Handle successful external transfer
     */
    private void handleExternalTransferSuccess(Transaction transaction, ExternalTransferCompletedEvent event) {
        log.info("Processing successful external transfer - TxID: {} - External Ref: {}",
                transaction.getTransactionId(),
                event.getExternalTransactionId());

        // Update transaction status
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCurrentStep(SagaStep.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setExternalTransactionId(event.getExternalTransactionId());
        transactionRepository.save(transaction);

        log.info("External transfer completed successfully - TxID: {}", transaction.getTransactionId());

        // TODO: Send success notification to user
    }

    /**
     * Handle failed external transfer - REFUND sender account
     */
    private void handleExternalTransferFailure(Transaction transaction, ExternalTransferCompletedEvent event) {
        log.warn("Processing failed external transfer - TxID: {} - Reason: {}",
                transaction.getTransactionId(),
                event.getMessage());

        // Calculate refund amount (amount + fee)
        BigDecimal refundAmount = transaction.getAmount().add(transaction.getFeeAmount());

        try {
            // Refund sender account (credit back)
            log.info("Refunding sender account {} - Amount: {}", 
                    transaction.getSenderAccountId(), 
                    refundAmount);

            accountServiceClient.creditAccount(
                    transaction.getSenderAccountId(),
                    refundAmount,
                    transaction.getTransactionId().toString(),
                    "Refund for failed external transfer: " + event.getMessage()
            );

            // Update transaction status
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCurrentStep(SagaStep.FAILED);
            transaction.setFailureReason("External transfer failed: " + event.getMessage());
            transaction.setExternalTransactionId(event.getExternalTransactionId());
            transactionRepository.save(transaction);

            log.info("External transfer failed and refunded - TxID: {}", transaction.getTransactionId());

            // TODO: Send failure notification to user

        } catch (Exception e) {
            // CRITICAL: Refund failed - needs manual intervention
            log.error("CRITICAL: Failed to refund sender account after external transfer failure - TxID: {} - Amount: {}",
                    transaction.getTransactionId(),
                    refundAmount,
                    e);

            transaction.setStatus(TransactionStatus.ROLLBACK_FAILED);
            transaction.setCurrentStep(SagaStep.ROLLBACK_FAILED);
            transaction.setFailureReason("External transfer failed and refund failed: " + e.getMessage());
            transactionRepository.save(transaction);

            // TODO: Trigger alert for manual reconciliation
            throw new RuntimeException("Failed to refund after external transfer failure", e);
        }
    }
}
