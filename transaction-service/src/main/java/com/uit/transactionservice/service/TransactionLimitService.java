package com.uit.transactionservice.service;

import com.uit.transactionservice.dto.response.TransactionLimitResponse;
import com.uit.transactionservice.entity.TransactionLimit;
import com.uit.transactionservice.mapper.TransactionMapper;
import com.uit.transactionservice.repository.TransactionLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

    private final TransactionLimitRepository transactionLimitRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Get transaction limits for an account
     */
    public TransactionLimitResponse getTransactionLimits(String accountId) {
        log.info("Getting transaction limits for account: {}", accountId);
        
        TransactionLimit limit = transactionLimitRepository.findByAccountId(accountId)
                .orElseGet(() -> createDefaultLimit(accountId));

        return transactionMapper.toLimitResponse(limit);
    }

    /**
     * Create default transaction limit
     */
    private TransactionLimit createDefaultLimit(String accountId) {
        TransactionLimit limit = TransactionLimit.builder()
                .accountId(accountId)
                .dailyLimit(BigDecimal.valueOf(50000))
                .monthlyLimit(BigDecimal.valueOf(200000))
                .dailyUsed(BigDecimal.ZERO)
                .monthlyUsed(BigDecimal.ZERO)
                .lastDailyReset(LocalDateTime.now())
                .lastMonthlyReset(LocalDateTime.now())
                .build();

        return transactionLimitRepository.save(limit);
    }
}
