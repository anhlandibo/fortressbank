package com.uit.externalbankmock.service;

import com.uit.externalbankmock.config.RabbitMQConfig;
import com.uit.externalbankmock.dto.TransferRequest;
import com.uit.externalbankmock.dto.TransferResponse;
import com.uit.externalbankmock.entity.ExternalTransfer;
import com.uit.externalbankmock.entity.TransferStatus;
import com.uit.externalbankmock.event.ExternalTransferCompletedEvent;
import com.uit.externalbankmock.repository.ExternalTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Mock External Bank Service
 * Simulates external bank behavior:
 * 1. Receive transfer request from RabbitMQ
 * 2. Process asynchronously (background job simulates processing)
 * 3. Random success/failure after 10-30 seconds
 * 4. Send callback to FortressBank via RabbitMQ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalBankService {

    private final ExternalTransferRepository transferRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    /**
     * Initiate transfer - immediately returns PENDING.
     * Actual processing happens asynchronously.
     */
    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Received external transfer request - TxID: {} - Amount: {} - To Bank: {}",
                request.getTransactionId(),
                request.getAmount(),
                request.getDestinationBankCode());

        // Check if already exists (idempotency)
        var existing = transferRepository.findByFortressBankTransactionId(request.getTransactionId());
        if (existing.isPresent()) {
            log.warn("Transfer already exists - TxID: {}", request.getTransactionId());
            return mapToResponse(existing.get());
        }

        // Create pending transfer
        ExternalTransfer transfer = ExternalTransfer.builder()
                .fortressBankTransactionId(request.getTransactionId())
                .sourceAccountNumber(request.getSourceAccountNumber())
                .sourceBankCode(request.getSourceBankCode())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .destinationBankCode(request.getDestinationBankCode())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(TransferStatus.PENDING)
                .message("Transfer request received, pending processing")
                .build();

        transfer = transferRepository.save(transfer);
        log.info("External transfer saved - External TxID: {} - Status: PENDING", transfer.getId());

        return mapToResponse(transfer);
    }

    /**
     * Query transfer status
     */
    public TransferResponse getTransferStatus(String externalTransactionId) {
        log.info("Querying transfer status - External TxID: {}", externalTransactionId);

        ExternalTransfer transfer = transferRepository.findById(externalTransactionId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + externalTransactionId));

        return mapToResponse(transfer);
    }

    /**
     * Background job: Process pending transfers asynchronously.
     * Runs every 10 seconds to simulate external bank processing.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    @Transactional
    public void processPendingTransfers() {
        List<ExternalTransfer> pendingTransfers = transferRepository.findByStatus(TransferStatus.PENDING);

        if (pendingTransfers.isEmpty()) {
            return;
        }

        log.info("Processing {} pending external transfers", pendingTransfers.size());

        for (ExternalTransfer transfer : pendingTransfers) {
            try {
                processTransfer(transfer);
            } catch (Exception e) {
                log.error("Error processing external transfer: {}", transfer.getId(), e);
            }
        }
    }

    /**
     * Process single transfer:
     * 1. Mark as PROCESSING
     * 2. Simulate processing delay (10-30 seconds)
     * 3. Random success (80%) or failure (20%)
     * 4. TODO: Send callback to FortressBank
     */
    private void processTransfer(ExternalTransfer transfer) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = transfer.getCreatedAt();

        // Check if enough time has passed (simulate processing time: 10-30 seconds)
        long secondsSinceCreation = java.time.Duration.between(createdAt, now).getSeconds();
        int processingTimeSeconds = 10 + random.nextInt(21); // 10-30 seconds

        if (secondsSinceCreation < processingTimeSeconds) {
            // Not ready yet, update to PROCESSING if still PENDING
            if (transfer.getStatus() == TransferStatus.PENDING) {
                transfer.setStatus(TransferStatus.PROCESSING);
                transfer.setMessage("Transfer is being processed by external bank");
                transferRepository.save(transfer);
                log.info("External transfer now PROCESSING - TxID: {}", transfer.getId());
            }
            return;
        }

        // Ready to complete - random success (80%) or failure (20%)
        boolean success = random.nextInt(100) < 80;

        if (success) {
            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setMessage("Transfer completed successfully");
            transfer.setProcessedAt(now);
            log.info("External transfer COMPLETED - TxID: {} - Amount: {}",
                    transfer.getId(),
                    transfer.getAmount());
        } else {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setMessage("Transfer failed: " + getRandomFailureReason());
            transfer.setProcessedAt(now);
            log.warn("External transfer FAILED - TxID: {} - Reason: {}",
                    transfer.getId(),
                    transfer.getMessage());
        }

        transferRepository.save(transfer);

        // Send callback to FortressBank via RabbitMQ
        sendCallbackToFortressBank(transfer);
    }

    /**
     * Send callback event to FortressBank transaction-service via RabbitMQ
     */
    private void sendCallbackToFortressBank(ExternalTransfer transfer) {
        try {
            ExternalTransferCompletedEvent event = ExternalTransferCompletedEvent.builder()
                    .externalTransactionId(transfer.getId())
                    .fortressBankTransactionId(transfer.getFortressBankTransactionId())
                    .status(transfer.getStatus().name())  // COMPLETED or FAILED
                    .amount(transfer.getAmount())
                    .sourceAccountNumber(transfer.getSourceAccountNumber())
                    .destinationAccountNumber(transfer.getDestinationAccountNumber())
                    .destinationBankCode(transfer.getDestinationBankCode())
                    .message(transfer.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Sending callback to FortressBank via MQ - FortressTxID: {} - Status: {}",
                    transfer.getFortressBankTransactionId(),
                    transfer.getStatus());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXTERNAL_TRANSFER_EXCHANGE,
                    RabbitMQConfig.EXTERNAL_TRANSFER_CALLBACK_ROUTING_KEY,
                    event
            );

            log.info("Callback sent successfully to FortressBank - FortressTxID: {}",
                    transfer.getFortressBankTransactionId());

        } catch (Exception e) {
            log.error("Failed to send callback to FortressBank - FortressTxID: {}",
                    transfer.getFortressBankTransactionId(), e);
            // Don't throw - transfer is already complete, just log error
        }
    }

    /**
     * Get random failure reason for simulation
     */
    private String getRandomFailureReason() {
        String[] reasons = {
                "Destination account not found",
                "Destination account closed",
                "Destination bank system error",
                "Invalid routing information",
                "Transaction limit exceeded"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    /**
     * Map entity to response DTO
     */
    private TransferResponse mapToResponse(ExternalTransfer transfer) {
        return TransferResponse.builder()
                .externalTransactionId(transfer.getId())
                .fortressBankTransactionId(transfer.getFortressBankTransactionId())
                .status(transfer.getStatus().name())
                .amount(transfer.getAmount())
                .sourceAccountNumber(transfer.getSourceAccountNumber())
                .destinationAccountNumber(transfer.getDestinationAccountNumber())
                .destinationBankCode(transfer.getDestinationBankCode())
                .message(transfer.getMessage())
                .timestamp(transfer.getCreatedAt())
                .build();
    }
}
