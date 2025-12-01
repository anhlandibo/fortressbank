package com.uit.accountservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransferResponse {
    
    private String transactionId;
    
    private String fromAccountId;
    private BigDecimal fromAccountOldBalance;
    private BigDecimal fromAccountNewBalance;
    
    private String toAccountId;
    private BigDecimal toAccountOldBalance;
    private BigDecimal toAccountNewBalance;
    
    private BigDecimal amount;
    private boolean success;
    private String message;
}
