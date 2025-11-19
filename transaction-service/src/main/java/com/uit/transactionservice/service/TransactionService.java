package com.uit.transactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.transactionservice.dto.request.CreateTransferRequest;
import com.uit.transactionservice.dto.response.TransactionResponse;
import com.uit.transactionservice.entity.*;
import com.uit.transactionservice.mapper.TransactionMapper;
import com.uit.transactionservice.repository.OutboxEventRepository;
import com.uit.transactionservice.repository.TransactionFeeRepository;
import com.uit.transactionservice.repository.TransactionLimitRepository;
import com.uit.transactionservice.repository.TransactionRepository;
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

    /**
     * Create a new transfer transaction
     */
    @Transactional
    public TransactionResponse createTransfer(CreateTransferRequest request, String userId) {
        log.info("Creating transfer from {} to {}", request.getFromAccountId(), request.getToAccountId());

        // 1. Check transaction limit
        checkTransactionLimit(request.getFromAccountId(), request.getAmount());

        // 2. Calculate fee
        BigDecimal fee = calculateFee(request.getType(), request.getAmount());
        BigDecimal totalAmount = request.getAmount().add(fee);

        // 3. Create transaction with PENDING status
        Transaction transaction = Transaction.builder()
                .senderAccountId(request.getFromAccountId())
                .receiverAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .feeAmount(fee)
                .txType(request.getType())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created with ID: {}", transaction.getTxId());

        // 4. Update transaction limit
        updateTransactionLimit(request.getFromAccountId(), totalAmount);

        // 5. Create outbox event for TransactionCreated
        createOutboxEvent(transaction, "TransactionCreated");

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
            payload.put("senderAccountId", transaction.getSenderAccountId());
            payload.put("receiverAccountId", transaction.getReceiverAccountId());
            payload.put("amount", transaction.getAmount());
            payload.put("feeAmount", transaction.getFeeAmount());
            payload.put("type", transaction.getTxType());
            payload.put("status", transaction.getStatus());
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
            log.info("Outbox event created: {} for transaction: {}", eventType, transaction.getTxId());

        } catch (Exception e) {
            log.error("Failed to create outbox event", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}
