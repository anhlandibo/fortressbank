package com.uit.transactionservice.service;

import com.uit.transactionservice.dto.request.UpdateFeeRequest;
import com.uit.transactionservice.dto.response.TransactionFeeResponse;
import com.uit.transactionservice.entity.TransactionFee;
import com.uit.transactionservice.entity.TransactionType;
import com.uit.transactionservice.mapper.TransactionMapper;
import com.uit.transactionservice.repository.TransactionFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionFeeService {

    private final TransactionFeeRepository transactionFeeRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Get all fee configurations
     */
    public List<TransactionFeeResponse> getAllFees() {
        log.info("Getting all fee configurations");
        return transactionFeeRepository.findAll().stream()
                .map(transactionMapper::toFeeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get fee configuration by transaction type
     */
    public TransactionFeeResponse getFeeByType(TransactionType type) {
        log.info("Getting fee configuration for type: {}", type);
        TransactionFee fee = transactionFeeRepository.findByTxType(type)
                .orElseThrow(() -> new RuntimeException("Fee configuration not found for type: " + type));
        
        return transactionMapper.toFeeResponse(fee);
    }

    /**
     * Update fee configuration
     */
    @Transactional
    public TransactionFeeResponse updateFee(TransactionType type, UpdateFeeRequest request) {
        log.info("Updating fee configuration for type: {}", type);
        
        TransactionFee fee = transactionFeeRepository.findByTxType(type)
                .orElseThrow(() -> new RuntimeException("Fee configuration not found for type: " + type));

        fee.setFeeAmount(request.getFeeAmount());

        fee = transactionFeeRepository.save(fee);
        log.info("Fee configuration updated for type: {}", type);

        return transactionMapper.toFeeResponse(fee);
    }
}
