package com.uit.transactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.transactionservice.dto.request.CreateTransferRequest;
import com.uit.transactionservice.dto.response.TransactionResponse;
import com.uit.transactionservice.entity.*;
import com.uit.transactionservice.mapper.TransactionMapper;
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
        log.info("Transaction created with ID: {} - Correlation ID: {} - Status: PENDING_OTP", 
                transaction.getTxId(), correlationId);

        // 4. Generate and save OTP to Redis
        String otpCode = otpService.generateOTP();
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
     * Verify OTP and complete transaction
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
                transactionRepository.save(transaction);
            } else if (result.getMessage().contains("Maximum")) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
            }
            throw new RuntimeException(result.getMessage());
        }

        // 5. Update transaction status and Saga step
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCurrentStep(com.uit.transactionservice.entity.SagaStep.OTP_VERIFIED);
        transaction = transactionRepository.save(transaction);
        log.info("OTP verified successfully - Transaction: {} moved to step: OTP_VERIFIED", transactionId);

        // 6. Update transaction limit
        BigDecimal totalAmount = transaction.getAmount().add(transaction.getFeeAmount());
        updateTransactionLimit(transaction.getSenderAccountId(), totalAmount);

        // 7. Create outbox event for TransactionCreated (Saga command to Account Service)
        // This will trigger DebitAccount command
        createOutboxEvent(transaction, "TransactionCreated");
        log.info("Saga orchestration started - Correlation ID: {}", transaction.getCorrelationId());

        return transactionMapper.toResponse(transaction);
    }
}
