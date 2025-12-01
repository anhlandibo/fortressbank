package com.uit.transactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.transactionservice.client.AccountServiceClient;
import com.uit.transactionservice.client.dto.AccountBalanceResponse;
import com.uit.transactionservice.client.dto.InternalTransferResponse;
import com.uit.transactionservice.dto.request.CreateTransferRequest;
import com.uit.transactionservice.dto.response.TransactionResponse;
import com.uit.transactionservice.entity.*;
import com.uit.transactionservice.event.ExternalTransferInitiatedEvent;
import com.uit.transactionservice.exception.AccountServiceException;
import com.uit.transactionservice.exception.InsufficientBalanceException;
import com.uit.transactionservice.mapper.TransactionMapper;
import com.uit.transactionservice.publisher.ExternalTransferPublisher;
import com.uit.transactionservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionFeeRepository transactionFeeRepository;
    private final TransactionLimitRepository transactionLimitRepository;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;
    private final OTPService otpService;
    private final AccountServiceClient accountServiceClient;
    private final ExternalTransferPublisher externalTransferPublisher;
    
    /**
     * Create a new transfer transaction with OTP verification
     */
    @Transactional
    public TransactionResponse createTransfer(CreateTransferRequest request, String userId, String phoneNumber) {
        log.info("Creating transfer from {} to {} with OTP", request.getFromAccountId(), request.getToAccountId());

        // 1. Check transaction limit
        checkTransactionLimit(request.getFromAccountId(), request.getAmount());

        // 2. Calculate fee
        BigDecimal fee = calculateFee(request.getType(), request.getAmount());
        // BigDecimal totalAmount = request.getAmount().add(fee);

        // 3. Create transaction with PENDING_OTP status and Saga state
        String correlationId = UUID.randomUUID().toString();
        
        Transaction transaction = Transaction.builder()
                .senderAccountId(request.getFromAccountId())
                .receiverAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .feeAmount(fee)
                .txType(request.getType())
                .status(TransactionStatus.PENDING_OTP)
                .description(request.getDescription())
                // Saga Orchestration Fields
                .correlationId(correlationId)
                .currentStep(com.uit.transactionservice.entity.SagaStep.STARTED)
                .build();
        transaction = transactionRepository.save(transaction);
        log.info("transaction " + transaction);

        log.info("Transaction created with ID: {} - Correlation ID: {} - Status: PENDING_OTP", 
                transaction.getTxId(), correlationId);

        // 4. Generate and save OTP to Redis
        String otpCode = otpService.generateOTP();
        log.info("Generated OTP Code: " + otpCode); // For development only
        otpService.saveOTP(transaction.getTxId(), otpCode, phoneNumber);
        
        log.info("OTP generated and saved to Redis for transaction: {}", transaction.getTxId());

        // 5. Send OTP SMS via event (notification-service will handle)
        sendOTPNotification(transaction.getTxId(), phoneNumber, otpCode);

        return transactionMapper.toResponse(transaction);
    }

    /**
     * Get transaction history by account
     */
    public Page<TransactionResponse> getTransactionHistory(String accountId, Pageable pageable) {
        log.info("Getting transaction history for account: {}", accountId);
        return transactionRepository.findBySenderAccountIdOrReceiverAccountId(accountId, accountId, pageable)
                .map(transactionMapper::toResponse);
    }

    /**
     * Get transaction history by status
     */
    public Page<TransactionResponse> getTransactionHistoryByStatus(TransactionStatus status, Pageable pageable) {
        log.info("Getting transaction history with status: {}", status);
        return transactionRepository.findByStatus(status, pageable)
                .map(transactionMapper::toResponse);
    }

    /**
     * Get transaction by ID
     */
    public TransactionResponse getTransactionById(UUID transactionId) {
        log.info("Getting transaction: {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return transactionMapper.toResponse(transaction);
    }

    /**
     * Calculate transaction fee
     */
    private BigDecimal calculateFee(TransactionType type, BigDecimal amount) {
        TransactionFee feeConfig = transactionFeeRepository.findByTxType(type)
                .orElseThrow(() -> new RuntimeException("Fee configuration not found for type: " + type));

        log.debug("Calculated fee: {} for amount: {} and type: {}", feeConfig.getFeeAmount(), amount, type);
        return feeConfig.getFeeAmount();
    }

    /**
     * Check if transaction exceeds limits
     */
    private void checkTransactionLimit(String accountId, BigDecimal amount) {
        TransactionLimit limit = transactionLimitRepository.findById(accountId)
                .orElseGet(() -> createDefaultLimit(accountId));

        // Reset limits if necessary
        resetLimitsIfNeeded(limit);

        // Check daily limit
        BigDecimal newDailyUsed = limit.getDailyUsed().add(amount);
        if (newDailyUsed.compareTo(limit.getDailyLimit()) > 0) {
            throw new RuntimeException("Daily transaction limit exceeded");
        }

        // Check monthly limit
        BigDecimal newMonthlyUsed = limit.getMonthlyUsed().add(amount);
        if (newMonthlyUsed.compareTo(limit.getMonthlyLimit()) > 0) {
            throw new RuntimeException("Monthly transaction limit exceeded");
        }
    }

    /**
     * Update transaction limit after successful transaction
     */
    private void updateTransactionLimit(String accountId, BigDecimal amount) {
        TransactionLimit limit = transactionLimitRepository.findById(accountId)
                .orElseGet(() -> createDefaultLimit(accountId));

        resetLimitsIfNeeded(limit);

        limit.setDailyUsed(limit.getDailyUsed().add(amount));
        limit.setMonthlyUsed(limit.getMonthlyUsed().add(amount));
        transactionLimitRepository.save(limit);

        log.debug("Updated transaction limit for account: {}", accountId);
    }

    /**
     * Create default transaction limit
     */
    private TransactionLimit createDefaultLimit(String accountId) {
        TransactionLimit limit = TransactionLimit.builder()
                .accountId(accountId)
                .dailyLimit(BigDecimal.valueOf(50000)) // Default $50,000
                .monthlyLimit(BigDecimal.valueOf(200000)) // Default $200,000
                .dailyUsed(BigDecimal.ZERO)
                .monthlyUsed(BigDecimal.ZERO)
                .lastDailyReset(LocalDateTime.now())
                .lastMonthlyReset(LocalDateTime.now())
                .build();

        return transactionLimitRepository.save(limit);
    }

    /**
     * Reset limits if period has passed
     */
    private void resetLimitsIfNeeded(TransactionLimit limit) {
        LocalDateTime now = LocalDateTime.now();

        // Reset daily if more than 24 hours
        if (limit.getLastDailyReset().plusDays(1).isBefore(now)) {
            limit.setDailyUsed(BigDecimal.ZERO);
            limit.setLastDailyReset(now);
        }

        // Reset monthly if more than 30 days
        if (limit.getLastMonthlyReset().plusDays(30).isBefore(now)) {
            limit.setMonthlyUsed(BigDecimal.ZERO);
            limit.setLastMonthlyReset(now);
        }
    }

    /**
     * Create outbox event
     */
    private void createOutboxEvent(Transaction transaction, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("transactionId", transaction.getTxId());
            payload.put("correlationId", transaction.getCorrelationId()); // For idempotency
            payload.put("senderAccountId", transaction.getSenderAccountId());
            payload.put("receiverAccountId", transaction.getReceiverAccountId());
            payload.put("amount", transaction.getAmount());
            payload.put("feeAmount", transaction.getFeeAmount());
            payload.put("type", transaction.getTxType());
            payload.put("status", transaction.getStatus());
            payload.put("currentStep", transaction.getCurrentStep());
            payload.put("timestamp", LocalDateTime.now());

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("transaction")
                    .aggregateId(transaction.getTxId().toString())
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxEventStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(event);
            log.info("Outbox event created: {} for transaction: {} with correlation ID: {}", 
                    eventType, transaction.getTxId(), transaction.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to create outbox event", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    /**
     * Send OTP notification via event
     */
    private void sendOTPNotification(UUID transactionId, String phoneNumber, String otpCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "OTPGenerated");
            payload.put("transactionId", transactionId.toString());
            payload.put("phoneNumber", phoneNumber);
            payload.put("otpCode", otpCode);
            payload.put("message", "Your OTP for transaction is: " + otpCode + ". Valid for 5 minutes.");
            payload.put("timestamp", LocalDateTime.now().toString());

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(transactionId.toString())
                    .aggregateType("Transaction")
                    .eventType("OTPGenerated")
                    .payload(payloadJson)
                    .status(OutboxEventStatus.PENDING)
                    .build();

            outboxEventRepository.save(event);
            log.info("OTP notification event created for transaction: {}", transactionId);

        } catch (Exception e) {
            log.error("Failed to create OTP notification event", e);
            // Don't throw exception, continue with transaction
        }
    }

    /**
     * Verify OTP and route to appropriate transfer handler
     */
    @Transactional
    public TransactionResponse verifyOTP(UUID transactionId, String otpCode) {
        log.info("Verifying OTP for transaction: {}", transactionId);

        // 1. Find transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

       
        // 2. Check transaction status
        if (transaction.getStatus() != TransactionStatus.PENDING_OTP) {
            throw new RuntimeException("Transaction is not pending OTP verification");
        }

        // 3. Verify OTP using OTPService
        OTPService.OTPVerificationResult result = otpService.verifyOTP(transactionId, otpCode);

        // 4. Handle verification result
        if (!result.isSuccess()) {
            if (result.getMessage().contains("expired")) {
                transaction.setStatus(TransactionStatus.OTP_EXPIRED);
                transaction.setFailureReason("OTP has expired. Please request a new OTP.");
                transaction = transactionRepository.save(transaction);
                log.warn("OTP expired for transaction: {}", transactionId);
            } else if (result.getMessage().contains("Maximum")) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Maximum OTP attempts exceeded.");
                transaction = transactionRepository.save(transaction);
                log.warn("Maximum OTP attempts reached for transaction: {}", transactionId);
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason(result.getMessage());
                transaction = transactionRepository.save(transaction);
                log.warn("OTP verification failed for transaction: {} - {}", transactionId, result.getMessage());
            }
            
            // Return response with failure reason instead of throwing exception
            return transactionMapper.toResponse(transaction);
        }

        // 5. Update transaction status to PENDING (OTP verified, processing payment)
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCurrentStep(SagaStep.OTP_VERIFIED);
        transaction = transactionRepository.save(transaction);
        log.info("OTP verified successfully - Transaction: {} moved to PENDING status", transactionId);

        // 6. Route to appropriate handler based on transfer type
        log.info("transaction type:" + transaction.getTxType() );
        if (transaction.isInternalTransfer()) {
            return processInternalTransfer(transaction);
        } else {
            return processExternalTransfer(transaction);
        }
    }

    /**
     * Process INTERNAL transfer (within FortressBank) - SYNCHRONOUS
     * Uses atomic endpoint for guaranteed consistency
     */
    private TransactionResponse processInternalTransfer(Transaction transaction) {
        log.info("Processing INTERNAL transfer - TxID: {}", transaction.getTxId());

        BigDecimal totalAmount = transaction.getAmount().add(transaction.getFeeAmount());
        
        try {
            // Use atomic internal transfer endpoint (recommended approach)
            log.info("Executing atomic internal transfer - From: {} To: {} Amount: {}", 
                    transaction.getSenderAccountId(), 
                    transaction.getReceiverAccountId(), 
                    totalAmount);
            
            InternalTransferResponse transferResponse = accountServiceClient.executeInternalTransfer(
                    transaction.getSenderAccountId(),
                    transaction.getReceiverAccountId(),
                    totalAmount,
                    transaction.getTxId().toString(),
                    transaction.getDescription()
            );

            // Update transaction with success
            transaction.setCurrentStep(SagaStep.COMPLETED);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            // Update transaction limit
            updateTransactionLimit(transaction.getSenderAccountId(), totalAmount);
            
            // Send success notification ASYNCHRONOUSLY (non-critical)
            sendTransactionNotification(transaction, "TransactionCompleted", true);
            
            log.info("Internal transfer completed - TxID: {} - Sender new balance: {} - Receiver new balance: {}",
                    transaction.getTxId(), 
                    transferResponse.getFromAccountNewBalance(),
                    transferResponse.getToAccountNewBalance());
            
            return transactionMapper.toResponse(transaction);

        } catch (InsufficientBalanceException e) {
            // Business logic error: insufficient balance
            log.error("Internal transfer {} failed: Insufficient balance", transaction.getTxId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCurrentStep(SagaStep.FAILED);
            transaction.setFailureReason("Insufficient balance");
            transactionRepository.save(transaction);
            
            sendTransactionNotification(transaction, "TransactionFailed", false);
            throw new RuntimeException("Insufficient balance in sender account", e);

        } catch (AccountServiceException e) {
            // Technical error: account service unavailable or error
            log.error("Internal transfer {} failed: Account service error", transaction.getTxId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCurrentStep(SagaStep.FAILED);
            transaction.setFailureReason("Account service error: " + e.getMessage());
            transactionRepository.save(transaction);
            
            sendTransactionNotification(transaction, "TransactionFailed", false);
            throw new RuntimeException("Failed to process internal transfer: " + e.getMessage(), e);

        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error during internal transfer: {}", transaction.getTxId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCurrentStep(SagaStep.FAILED);
            transaction.setFailureReason("Unexpected error: " + e.getMessage());
            transactionRepository.save(transaction);
            
            sendTransactionNotification(transaction, "TransactionFailed", false);
            throw new RuntimeException("Unexpected error during transfer", e);
        }
    }

    /**
     * Process EXTERNAL transfer (to another bank) - ASYNCHRONOUS via RabbitMQ
     * Only debits sender account immediately, publishes event to MQ, credit happens later via callback
     */
    private TransactionResponse processExternalTransfer(Transaction transaction) {
        log.info("Processing EXTERNAL transfer - TxID: {} - To Bank: {}", 
                transaction.getTxId(), transaction.getDestinationBankCode());

        BigDecimal totalAmount = transaction.getAmount().add(transaction.getFeeAmount());
        
        try {
            // Step 1: Debit sender account first (sync)
            log.info("Step 1: Debiting sender account {} - Amount: {}", 
                    transaction.getSenderAccountId(), totalAmount);
            
            AccountBalanceResponse debitResponse = accountServiceClient.debitAccount(
                    transaction.getSenderAccountId(),
                    totalAmount,
                    transaction.getTxId().toString(),
                    "External transfer to " + transaction.getDestinationBankCode()
            );
            
            transaction.setCurrentStep(SagaStep.DEBIT_COMPLETED);
            transaction = transactionRepository.save(transaction);
            log.info("Debit completed - New sender balance: {}", debitResponse.getNewBalance());

            // Step 2: Publish external transfer event to RabbitMQ (async)
            log.info("Step 2: Publishing external transfer event to message queue");
            
            ExternalTransferInitiatedEvent event = ExternalTransferInitiatedEvent.builder()
                    .transactionId(transaction.getTxId().toString())
                    .sourceAccountNumber(transaction.getSenderAccountId())
                    .sourceBankCode("FORTRESS")
                    .destinationAccountNumber(transaction.getReceiverAccountId())
                    .destinationBankCode(transaction.getDestinationBankCode())
                    .amount(transaction.getAmount())
                    .description(transaction.getDescription())
                    .timestamp(LocalDateTime.now())
                    .build();

            externalTransferPublisher.publishExternalTransferInitiated(event);
            
            // Update transaction status - waiting for callback
            transaction.setCurrentStep(SagaStep.EXTERNAL_INITIATED);
            transaction.setStatus(TransactionStatus.PENDING);  // Still pending until callback
            transaction = transactionRepository.save(transaction);

            log.info("External transfer event published to MQ - TxID: {} - Status: PENDING", 
                    transaction.getTxId());

            // Update transaction limit
            updateTransactionLimit(transaction.getSenderAccountId(), totalAmount);

            // Send notification that transfer is being processed
            sendTransactionNotification(transaction, "ExternalTransferInitiated", false);
            
            return transactionMapper.toResponse(transaction);

        } catch (InsufficientBalanceException e) {
            log.error("External transfer {} failed: Insufficient balance", transaction.getTxId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCurrentStep(SagaStep.FAILED);
            transaction.setFailureReason("Insufficient balance");
            transactionRepository.save(transaction);
            
            sendTransactionNotification(transaction, "TransactionFailed", false);
            throw new RuntimeException("Insufficient balance in sender account", e);

        } catch (AccountServiceException e) {
            // Handle partial failure - need rollback
            log.error("Transaction {} failed during account service call", transaction.getTxId(), e);
            
            // If debit succeeded but credit failed, rollback the debit
            if (transaction.getCurrentStep() == SagaStep.DEBIT_COMPLETED) {
                log.warn("Rolling back debit for transaction {}", transaction.getTxId());
                try {
                    accountServiceClient.rollbackDebit(
                            transaction.getSenderAccountId(),
                            totalAmount,
                            transaction.getTxId().toString()
                    );
                    transaction.setCurrentStep(SagaStep.ROLLBACK_COMPLETED);
                    log.info("Rollback completed for transaction {}", transaction.getTxId());
                } catch (Exception rollbackError) {
                    log.error("CRITICAL: Rollback failed for transaction {} - Manual intervention required!", 
                            transaction.getTxId(), rollbackError);
                    transaction.setCurrentStep(SagaStep.ROLLBACK_FAILED);
                }
            }
            
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            
            // Send failure notification asynchronously
            sendTransactionNotification(transaction, "TransactionFailed", false);
            
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);

        } catch (Exception e) {
            // Unexpected error
            log.error("Unexpected error during transaction {}", transaction.getTxId(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCurrentStep(SagaStep.FAILED);
            transaction.setFailureReason("System error: " + e.getMessage());
            transactionRepository.save(transaction);
            
            // Send failure notification asynchronously
            sendTransactionNotification(transaction, "TransactionFailed", false);
            
            throw new RuntimeException("Transaction failed due to system error", e);
        }
    }

    /**
     * Send transaction notification asynchronously (non-blocking)
     */
    private void sendTransactionNotification(Transaction transaction, String eventType, boolean success) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("transactionId", transaction.getTxId().toString());
            payload.put("senderAccountId", transaction.getSenderAccountId());
            payload.put("receiverAccountId", transaction.getReceiverAccountId());
            payload.put("amount", transaction.getAmount());
            payload.put("status", transaction.getStatus().toString());
            payload.put("success", success);
            payload.put("message", success ? "Transaction completed successfully" : "Transaction failed: " + transaction.getFailureReason());
            payload.put("timestamp", LocalDateTime.now().toString());

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(transaction.getTxId().toString())
                    .aggregateType("Transaction")
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxEventStatus.PENDING)
                    .build();

            outboxEventRepository.save(event);
            log.info("Notification event {} created for transaction: {}", eventType, transaction.getTxId());

        } catch (Exception e) {
            log.error("Failed to create notification event - non-critical", e);
            // Don't throw exception, notification is non-critical
        }
    }
}
